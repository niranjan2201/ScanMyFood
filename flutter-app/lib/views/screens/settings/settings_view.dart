import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:read_the_label/services/auth_service.dart';
import 'package:read_the_label/theme/app_colors.dart';
import 'package:read_the_label/theme/app_text_styles.dart';
import 'package:read_the_label/viewmodels/daily_intake_view_model.dart';
import 'package:read_the_label/views/widgets/daily_intake/edit_nutrient_goals_sheet.dart';

class SettingsView extends StatelessWidget {
  const SettingsView({super.key});

  @override
  Widget build(BuildContext context) {
    final dailyIntakeVm = context.watch<DailyIntakeViewModel>();

    return Scaffold(
      backgroundColor: AppColors.background,
      appBar: AppBar(
        title: Text(
          'Settings',
          style: AppTextStyles.heading2BoldClose,
        ),
        backgroundColor: AppColors.surface,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back_ios_new, color: AppColors.primaryWhite),
          onPressed: () => Navigator.of(context).pop(),
        ),
        centerTitle: true,
      ),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'NUTRITION & TARGETS',
              style: AppTextStyles.caption.copyWith(
                color: AppColors.secondaryLabel,
                fontWeight: FontWeight.w700,
                letterSpacing: 1.0,
              ),
            ),
            const SizedBox(height: 10),
            Container(
              decoration: BoxDecoration(
                color: AppColors.cardBackground,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(
                  color: Colors.white.withValues(alpha: 0.05),
                  width: 1,
                ),
              ),
              child: ListTile(
                leading: Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppColors.saturatedOrange.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Icon(Icons.tune_rounded, color: AppColors.saturatedOrange, size: 22),
                ),
                title: Text(
                  'Daily Nutrition Goals',
                  style: AppTextStyles.bodyLargeBold.copyWith(
                    color: AppColors.primaryWhite,
                    fontSize: 16,
                  ),
                ),
                subtitle: Text(
                  'Calories: ${dailyIntakeVm.calorieGoal.toInt()} kcal • P: ${dailyIntakeVm.proteinGoal.toInt()}g • C: ${dailyIntakeVm.carbsGoal.toInt()}g • F: ${dailyIntakeVm.fatGoal.toInt()}g',
                  style: AppTextStyles.bodySmall.copyWith(
                    color: AppColors.secondaryLabel,
                    fontSize: 12,
                  ),
                ),
                trailing: const Icon(Icons.arrow_forward_ios, size: 14, color: AppColors.secondaryLabel),
                onTap: () => EditNutrientGoalsSheet.show(context),
              ),
            ),
            const Spacer(),
            // Logout Button
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => _handleLogout(context),
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppColors.secondaryRed,
                  foregroundColor: AppColors.primaryWhite,
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  elevation: 0,
                ),
                child: Text(
                  'Logout',
                  style: AppTextStyles.bodyMediumBold.copyWith(
                    fontSize: 18,
                    letterSpacing: 1.2,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  void _handleLogout(BuildContext context) async {
    final authService = context.read<AuthService>();
    
    // Show confirmation dialog (optional, but good practice)
    final bool? confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        backgroundColor: AppColors.cardBackground,
        title: Text('Logout', style: AppTextStyles.heading3Bold),
        content: const Text(
          'Are you sure you want to log out?',
          style: TextStyle(color: AppColors.primaryWhite),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            child: const Text('Cancel', style: TextStyle(color: AppColors.textSecondary)),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Logout', style: TextStyle(color: AppColors.secondaryRed)),
          ),
        ],
      ),
    );

    if (confirm == true) {
      // Perform sign out
      await authService.signOut();
      
      // Navigation back happens automatically because of StreamProvider in main.dart
      // but we need to pop the settings screen if it's still there
      if (context.mounted) {
        Navigator.of(context).pop();
      }
    }
  }
}
