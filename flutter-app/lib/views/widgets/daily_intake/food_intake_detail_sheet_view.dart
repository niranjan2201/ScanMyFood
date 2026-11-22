import 'dart:ui';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'package:provider/provider.dart';
import 'package:share_plus/share_plus.dart';
import 'package:read_the_label/core/constants/app_constants.dart';
import 'package:read_the_label/theme/app_colors.dart';
import 'package:read_the_label/theme/app_text_styles.dart';
import 'package:read_the_label/viewmodels/daily_intake_view_model.dart';
import 'package:read_the_label/viewmodels/ui_view_model.dart';
import 'package:read_the_label/views/widgets/daily_intake/food_history_item_card.dart';
import 'package:read_the_label/views/widgets/daily_intake/food_item_card_shimmer.dart';
import 'package:read_the_label/views/widgets/common/total_nutrients_card.dart';
import 'package:read_the_label/views/widgets/daily_intake/total_nutrients_card_shimmer.dart';
import 'package:read_the_label/views/widgets/daily_intake/list_tile.dart';
import 'package:read_the_label/views/widgets/daily_intake/high_low_nutrient_indicator.dart';
import 'package:read_the_label/utils/nutrient_utils.dart';
import 'package:soft_edge_blur/soft_edge_blur.dart';

class FoodIntakeDetailSheetView extends StatefulWidget {
  const FoodIntakeDetailSheetView({super.key, required this.itemCard});

  final FoodHistoryItemCard itemCard;

  @override
  State<FoodIntakeDetailSheetView> createState() =>
      _FoodIntakeDetailSheetViewState();
}

class _FoodIntakeDetailSheetViewState extends State<FoodIntakeDetailSheetView> {
  Color? _dominantColor;
  bool _isFavorited = false;

  @override
  void initState() {
    super.initState();
    _extractColor();
  }

  Future<void> _extractColor() async {
    final imageUrl = widget.itemCard.item.imageUrl;
    if (imageUrl == null || imageUrl.isEmpty || imageUrl.startsWith('GENERATING')) return;

    final vm = context.read<UiViewModel>();
    final color = await vm.extractDominantColor(imageUrl);

    if (mounted && color != null) {
      setState(() {
        _dominantColor = color;
      });
    }
  }

  void _handleShare() {
    final item = widget.itemCard.item;
    final mealName = item.intakeName ?? 'Meal';
    final calories = item.caloriesValue > 0 ? item.caloriesValue.round() : item.energyValue.round();
    final protein = item.proteinValue.round();
    final carbs = item.totalCarbohydrateValue.round();
    final fat = item.totalFatValue.round();

    final shareText = '''🍽️ $mealName
🔥 Energy: $calories kcal
💪 Protein: ${protein}g
🍞 Carbs: ${carbs}g
🥑 Fat: ${fat}g

Tracked with ScanMyFood 🥗''';

    Share.share(shareText, subject: 'My Meal: $mealName');
  }

  void _handleDelete() async {
    final item = widget.itemCard.item;
    final mealName = item.intakeName ?? 'this meal';

    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        title: Text(
          'Delete Meal',
          style: AppTextStyles.heading3Bold.copyWith(color: AppColors.primaryWhite),
        ),
        content: Text(
          'Are you sure you want to delete "$mealName" from your daily intake log?',
          style: AppTextStyles.bodyMedium.copyWith(color: AppColors.secondaryLabel),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancel', style: TextStyle(color: AppColors.secondaryLabel)),
          ),
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text(
              'Delete',
              style: TextStyle(color: Colors.redAccent, fontWeight: FontWeight.bold),
            ),
          ),
        ],
      ),
    );

    if (confirm == true && mounted) {
      final vm = context.read<DailyIntakeViewModel>();
      if (item.id != null) {
        final success = await vm.deleteIntake(item.id!);
        if (mounted) {
          Navigator.of(context).pop(); // Close detail sheet
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                success ? '"$mealName" deleted from daily intake' : 'Failed to delete "$mealName"',
              ),
              backgroundColor: success ? Colors.redAccent : AppColors.cardBackground,
              duration: const Duration(seconds: 2),
            ),
          );
        }
      }
    }
  }

  Widget _buildHeaderImage(String? imageUrl) {
    final bool hasValidImage = imageUrl != null &&
        imageUrl.isNotEmpty &&
        !imageUrl.startsWith('GENERATING') &&
        (imageUrl.startsWith('http://') || imageUrl.startsWith('https://'));

    if (hasValidImage) {
      return SoftEdgeBlur(
        edges: [
          EdgeBlur(
            type: EdgeType.bottomEdge,
            size: 220,
            sigma: 8,
            tintColor: _dominantColor ?? Colors.black.withValues(alpha: 0.4),
            controlPoints: [
              ControlPoint(
                position: 0.2,
                type: ControlPointType.visible,
              ),
              ControlPoint(
                position: 1,
                type: ControlPointType.transparent,
              ),
            ],
          ),
        ],
        child: Image.network(
          imageUrl,
          fit: BoxFit.cover,
          height: 380,
          width: double.infinity,
          errorBuilder: (context, error, stackTrace) => _buildFallbackBanner(),
          loadingBuilder: (context, child, loadingProgress) {
            if (loadingProgress == null) return child;
            return Container(
              height: 380,
              width: double.infinity,
              color: Colors.grey.shade900,
              child: Center(
                child: CircularProgressIndicator(
                  value: loadingProgress.expectedTotalBytes != null
                      ? loadingProgress.cumulativeBytesLoaded /
                          loadingProgress.expectedTotalBytes!
                      : null,
                ),
              ),
            );
          },
        ),
      );
    }

    return _buildFallbackBanner();
  }

  Widget _buildFallbackBanner() {
    return Container(
      height: 320,
      width: double.infinity,
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: [
            _dominantColor ?? const Color(0xFF2E2638),
            const Color(0xFF1E1B28),
            AppColors.background,
          ],
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
        ),
      ),
      child: Center(
        child: Container(
          padding: const EdgeInsets.all(22),
          decoration: BoxDecoration(
            color: Colors.white.withValues(alpha: 0.08),
            shape: BoxShape.circle,
            border: Border.all(
              color: Colors.white.withValues(alpha: 0.15),
              width: 1.5,
            ),
          ),
          child: const Icon(
            Icons.restaurant_rounded,
            size: 56,
            color: AppColors.saturatedOrange,
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final bgColor = _dominantColor ?? AppColors.background;
    final topPadding = MediaQuery.of(context).padding.top;

    return Material(
      color: bgColor,
      child: CupertinoPageScaffold(
        backgroundColor: bgColor,
        child: Stack(
          children: [
            SingleChildScrollView(
              child: Column(
                children: [
                  Stack(
                    children: [
                      _buildHeaderImage(widget.itemCard.item.imageUrl),
                      Positioned(
                        bottom: 30,
                        left: 20,
                        right: 20,
                        child: Builder(
                          builder: (context) {
                            final vm = context.watch<DailyIntakeViewModel>();
                            if (vm.loading) return const SizedBox.shrink();
                            return Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              crossAxisAlignment: CrossAxisAlignment.center,
                              spacing: 10,
                              children: [
                                Text(
                                  vm.scannedMealName,
                                  textAlign: TextAlign.center,
                                  style: AppTextStyles.heading2Bold.copyWith(
                                    color: AppColors.getTitleColor(
                                      _dominantColor ?? Colors.black,
                                    ),
                                    fontWeight: FontWeight.w600,
                                    height: 1.15,
                                  ),
                                  maxLines: 2,
                                  overflow: TextOverflow.ellipsis,
                                ),
                                Builder(
                                  builder: (context) {
                                    final portionVal = widget.itemCard.item.portion;
                                    final portionStr = portionVal % 1 == 0
                                        ? portionVal.toInt().toString()
                                        : portionVal.toString();
                                    final servingText =
                                        '$portionStr ${portionVal == 1.0 ? "Serving" : "Servings"}';
                                    final timeText = widget.itemCard.item.createdAt != null
                                        ? DateFormat('h:mm a')
                                            .format(widget.itemCard.item.createdAt!)
                                        : '';
                                    final subtitleText = timeText.isNotEmpty
                                        ? '$servingText • $timeText'
                                        : servingText;

                                    return Text(
                                      subtitleText,
                                      textAlign: TextAlign.center,
                                      style: AppTextStyles.bodyMedium.copyWith(
                                        color: AppColors.getSubtitleColor(
                                          _dominantColor ?? Colors.black,
                                        ),
                                        fontSize: 12,
                                        fontWeight: FontWeight.w500,
                                        letterSpacing: 0.2,
                                        height: 1.2,
                                      ),
                                    );
                                  },
                                ),
                                Row(
                                  mainAxisAlignment: MainAxisAlignment.center,
                                  children: [
                                    // Share Pill Button
                                    ElevatedButton.icon(
                                      onPressed: _handleShare,
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: Colors.white,
                                        foregroundColor: Colors.black,
                                        elevation: 0,
                                        padding: const EdgeInsets.symmetric(
                                          vertical: 10,
                                          horizontal: 24,
                                        ),
                                        shape: RoundedRectangleBorder(
                                          borderRadius: BorderRadius.circular(30),
                                        ),
                                      ),
                                      icon: const Icon(
                                        CupertinoIcons.share,
                                        size: 18,
                                        color: Colors.black,
                                      ),
                                      label: Text(
                                        "Share",
                                        style: AppTextStyles.bodyMediumBold.copyWith(
                                          fontSize: 15,
                                          color: Colors.black,
                                        ),
                                      ),
                                    ),
                                    const SizedBox(width: 12),
                                    // Right Heart Button
                                    ClipRRect(
                                      borderRadius: BorderRadius.circular(25),
                                      child: BackdropFilter(
                                        filter: ImageFilter.blur(
                                          sigmaX: 5,
                                          sigmaY: 5,
                                        ),
                                        child: Container(
                                          decoration: BoxDecoration(
                                            color: Colors.white.withValues(
                                              alpha: 0.1,
                                            ),
                                            shape: BoxShape.circle,
                                            border: Border.all(
                                              color: AppColors.primaryWhite
                                                  .withValues(alpha: 0.2),
                                              width: 1,
                                            ),
                                          ),
                                          child: IconButton(
                                            padding: EdgeInsets.zero,
                                            icon: Icon(
                                              _isFavorited
                                                  ? CupertinoIcons.heart_solid
                                                  : CupertinoIcons.heart,
                                              color: _isFavorited
                                                  ? Colors.redAccent
                                                  : Colors.white,
                                              size: 22,
                                            ),
                                            onPressed: () {
                                              setState(() {
                                                _isFavorited = !_isFavorited;
                                              });
                                            },
                                          ),
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              ],
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                  Builder(
                    builder: (context) {
                      final isLoading = context.select(
                        (DailyIntakeViewModel vm) => vm.loading,
                      );

                      if (isLoading) {
                        return Container(
                          width: double.infinity,
                          padding: const EdgeInsets.symmetric(horizontal: 20),
                          child: const Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              FoodItemCardShimmer(),
                              FoodItemCardShimmer(),
                              TotalNutrientsCardShimmer(),
                            ],
                          ),
                        );
                      }

                      final dailyIntakeProvider = context.read<DailyIntakeViewModel>();
                      return Container(
                        width: double.infinity,
                        padding: const EdgeInsets.symmetric(horizontal: 16),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          spacing: 16,
                          children: [
                            Builder(
                              builder: (context) {
                                final targetNutrients = <String>[
                                  AppConstants.protein,
                                  AppConstants.totalCarbohydrate,
                                  AppConstants.dietaryFiber,
                                  AppConstants.totalFat,
                                  AppConstants.totalSugars,
                                ];

                                final indicators = dailyIntakeProvider.nutrientInfo.where((n) {
                                  final name = n['name'] as String?;
                                  final status = n['dv_status'] as String?;
                                  return name != null &&
                                      targetNutrients.contains(
                                        NutrientUtils.toSnakeCase(name),
                                      ) &&
                                      (status == 'High' || status == 'Low');
                                }).toList();

                                if (indicators.isEmpty) {
                                  return const SizedBox.shrink();
                                }

                                return Column(
                                  spacing: 20,
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Wrap(
                                      spacing: 8,
                                      runSpacing: 8,
                                      children: indicators.map((nutrient) {
                                        return HighLowNutrientIndicator(
                                          nutrientName: nutrient['name'],
                                          dvStatus: nutrient['dv_status'],
                                          healthImpact: nutrient['health_impact'],
                                          quantity: nutrient['quantity'],
                                          unit: nutrient['unit'],
                                          dominantColor: _dominantColor,
                                        );
                                      }).toList(),
                                    ),
                                  ],
                                );
                              },
                            ),
                            // Group Includes title and list
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const SizedBox(height: 8),
                                Text(
                                  "Includes",
                                  style: AppTextStyles.heading2Close.copyWith(
                                    color: AppColors.primaryWhite,
                                  ),
                                ),
                                const SizedBox(height: 4),
                                Column(
                                  children: dailyIntakeProvider.analyzedScannedFoodItems
                                      .asMap()
                                      .entries
                                      .map(
                                        (entry) => AppListTile(
                                          item: entry.value,
                                          index: entry.key,
                                          dominantColor: _dominantColor,
                                        ),
                                      )
                                      .toList(),
                                ),
                              ],
                            ),

                            Padding(
                              padding: const EdgeInsets.only(bottom: 24.0),
                              child: TotalNutrientsCard(
                                source: AppConstants.scanMeal,
                                foodAnalysis: dailyIntakeProvider.intakeDetails,
                                mealName: dailyIntakeProvider.scannedMealName,
                                numberOfFoodItems:
                                    dailyIntakeProvider.analyzedScannedFoodItems.length,
                                totalPlateNutrients:
                                    dailyIntakeProvider.totalScannedPlateNutrients,
                                nutrientInfo: dailyIntakeProvider.nutrientInfo,
                                showSaveOptions: false,
                              ),
                            ),
                          ],
                        ),
                      );
                    },
                  ),
                ],
              ),
            ),
            // Persistent Header Buttons (Close, Share, Delete)
            Positioned(
              top: topPadding > 0 ? topPadding + 6 : 14,
              left: 14,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(25),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.35),
                      shape: BoxShape.circle,
                      border: Border.all(
                        color: AppColors.primaryWhite.withValues(alpha: 0.2),
                        width: 1,
                      ),
                    ),
                    child: IconButton(
                      onPressed: () => Navigator.of(context).pop(),
                      icon: const Icon(
                        Icons.close,
                        color: AppColors.primaryWhite,
                        size: 24,
                      ),
                    ),
                  ),
                ),
              ),
            ),
            Positioned(
              top: topPadding > 0 ? topPadding + 6 : 14,
              right: 14,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(25),
                child: BackdropFilter(
                  filter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
                  child: Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.35),
                      borderRadius: BorderRadius.circular(30),
                      border: Border.all(
                        color: AppColors.primaryWhite.withValues(alpha: 0.2),
                        width: 1,
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        IconButton(
                          onPressed: _handleShare,
                          tooltip: 'Share Meal',
                          icon: const Icon(
                            CupertinoIcons.share,
                            color: AppColors.primaryWhite,
                            size: 22,
                          ),
                        ),
                        IconButton(
                          onPressed: _handleDelete,
                          tooltip: 'Delete Meal',
                          icon: const Icon(
                            CupertinoIcons.delete,
                            color: Colors.redAccent,
                            size: 22,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
