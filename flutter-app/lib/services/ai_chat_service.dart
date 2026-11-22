import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_dotenv/flutter_dotenv.dart';
import 'package:genui/genui.dart' hide TextPart;
import 'package:genui/genui.dart' as genui;
import 'package:uuid/uuid.dart';
import 'package:read_the_label/models/user_profile.dart';
import 'package:read_the_label/services/local_database_service.dart';
import 'package:read_the_label/services/ai_context_builder.dart';
import 'package:read_the_label/services/tools/ai_tool_declarations.dart';
import 'package:read_the_label/services/tools/tool_execution_client.dart';
import 'package:read_the_label/main.dart';

class AgentStepEvent {
  final String sessionId;
  final String toolName;
  final String label;
  final bool isRunning;
  final bool success;
  final DateTime timestamp;

  AgentStepEvent({
    required this.sessionId,
    required this.toolName,
    required this.label,
    required this.isRunning,
    this.success = true,
    required this.timestamp,
  });
}

class AiChatService {
  static final AiChatService instance = AiChatService._init();

  final _activeHistories = <String, List<Map<String, dynamic>>>{};
  final _stepEventsController = StreamController<AgentStepEvent>.broadcast();

  AiChatService._init();

  Stream<AgentStepEvent> get stepEvents => _stepEventsController.stream;

  Map<String, dynamic> _chatMessageToRestContent(ChatMessage msg) {
    final parts = <Map<String, dynamic>>[];
    for (final part in msg.parts) {
      if (part is genui.TextPart) {
        parts.add({'text': part.text});
      }
    }
    return {
      'role': msg.role == ChatMessageRole.user ? 'user' : 'model',
      'parts': parts,
    };
  }

  Future<List<Map<String, dynamic>>> _getOrCreateHistory(
    String sessionId,
  ) async {
    if (_activeHistories.containsKey(sessionId)) {
      return _activeHistories[sessionId]!;
    }

    final dbMessages = await LocalDatabaseService.instance.getSessionMessages(
      sessionId,
    );

    final history = dbMessages
        .where((m) => m.role != ChatMessageRole.system)
        .map(_chatMessageToRestContent)
        .toList();

    _activeHistories[sessionId] = history;
    return history;
  }

  Future<http.Response> _callGeminiApi({
    required String apiKey,
    required Map<String, dynamic> payload,
  }) async {
    final candidateModels = [
      'gemini-3.5-flash-lite',
      'gemini-3.5-flash',
      'gemini-3.8-flash',
    ];

    http.Response? lastResponse;

    for (final model in candidateModels) {
      final url = Uri.parse(
        'https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey',
      );

      try {
        final response = await http.post(
          url,
          headers: {'Content-Type': 'application/json'},
          body: jsonEncode(payload),
        ).timeout(const Duration(seconds: 25));

        if (response.statusCode == 200) {
          return response;
        }

        logger.w('Model $model returned status ${response.statusCode}: ${response.body}');
        lastResponse = response;
      } catch (e) {
        logger.w('Error calling Gemini API on model $model: $e');
      }
    }

    if (lastResponse != null) {
      return lastResponse;
    }
    throw Exception('All Gemini model endpoints failed.');
  }

  /// Sends a message and processes the response.
  /// Emits step events during the agentic loop for tool calls.
  Future<void> processMessage({
    required String sessionId,
    required ChatMessage userMessage,
    required A2uiTransportAdapter transport,
    required ToolExecutionClient toolClient,
    UserProfile? profile,
    String? mealScanContext,
  }) async {
    final db = LocalDatabaseService.instance;
    final messageId = const Uuid().v4();

    // 1. Save user message to database
    await db.saveMessage(sessionId, messageId, userMessage);
    await db.updateSessionLastMsgAt(sessionId, DateTime.now());

    try {
      final history = await _getOrCreateHistory(sessionId);
      history.add(_chatMessageToRestContent(userMessage));

      // Build system instruction
      final systemPrompt = AiContextBuilder.buildSystemPrompt(
        profile: profile,
        mealScanContext: mealScanContext,
      );

      final apiKey = dotenv.env['GEMINI_API_KEY'] ??
          const String.fromEnvironment('GEMINI_API_KEY');

      if (apiKey.isEmpty) {
        throw Exception('GEMINI_API_KEY is not configured in .env');
      }

      // Handle function calls in the agentic loop
      int iterations = 0;
      const maxIterations = 5;

      while (iterations < maxIterations) {
        iterations++;

        final payload = <String, dynamic>{
          'system_instruction': {
            'parts': [
              {'text': systemPrompt}
            ]
          },
          'contents': history,
          'tools': AiToolDeclarations.restTools,
        };

        final response = await _callGeminiApi(
          apiKey: apiKey,
          payload: payload,
        );

        if (response.statusCode != 200) {
          throw Exception(
            'Gemini API request failed with status ${response.statusCode}: ${response.body}',
          );
        }

        final data = jsonDecode(response.body) as Map<String, dynamic>;
        final candidates = data['candidates'] as List?;
        if (candidates == null || candidates.isEmpty) {
          throw Exception('No candidates returned from Gemini API');
        }

        final candidate = candidates.first['content'] as Map<String, dynamic>;
        // Preserve model response including thoughtSignature and functionCall in history
        history.add(candidate);

        final parts = (candidate['parts'] as List?) ?? [];
        final funcResponseParts = <Map<String, dynamic>>[];

        for (final part in parts) {
          if (part is Map && part.containsKey('functionCall')) {
            final call = Map<String, dynamic>.from(part['functionCall'] as Map);
            final toolName = call['name'] as String;
            final args = call['args'] != null
                ? Map<String, dynamic>.from(call['args'] as Map)
                : <String, dynamic>{};

            // Emit start event
            final humanLabel = AiToolDeclarations.humanLabel(toolName, args);
            _stepEventsController.add(
              AgentStepEvent(
                sessionId: sessionId,
                toolName: toolName,
                label: humanLabel,
                isRunning: true,
                timestamp: DateTime.now(),
              ),
            );

            Map<String, Object?> result;
            bool success = true;
            try {
              // Execute tool on Spring Boot
              result = await toolClient.execute(toolName, args);
            } catch (e) {
              success = false;
              result = {'error': e.toString()};
            }

            // Emit completed event
            _stepEventsController.add(
              AgentStepEvent(
                sessionId: sessionId,
                toolName: toolName,
                label: success ? humanLabel : 'Failed',
                isRunning: false,
                success: success,
                timestamp: DateTime.now(),
              ),
            );

            funcResponseParts.add({
              'functionResponse': {
                'name': toolName,
                'response': result,
              }
            });
          }
        }

        // If there were function calls, feed the responses back into the conversation
        if (funcResponseParts.isNotEmpty) {
          history.add({
            'role': 'user',
            'parts': funcResponseParts,
          });
          continue; // Loop again for the model to see results and summarize
        }

        // If no function calls, extract text response
        final sb = StringBuffer();
        for (final part in parts) {
          if (part is Map && part['text'] != null) {
            sb.write(part['text']);
          }
        }

        final aiText = sb.toString().trim();
        if (aiText.isNotEmpty) {
          transport.addChunk(aiText);

          // Save model response to database
          final aiMessageId = const Uuid().v4();
          final aiMessage = ChatMessage.model(aiText);
          await db.saveMessage(sessionId, aiMessageId, aiMessage);
          await db.updateSessionLastMsgAt(sessionId, DateTime.now());
        }
        break; // Successfully completed turn
      }
    } catch (e, stack) {
      logger.e('Error processing message in session $sessionId: $e', e, stack);
      transport.addChunk(
        'Sorry, I encountered an error processing your request. Please try again.',
      );
    }
  }

  void clearSessionCache(String sessionId) {
    _activeHistories.remove(sessionId);
  }
}
