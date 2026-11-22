import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:read_the_label/core/constants/app_constants.dart';
import 'package:read_the_label/theme/app_colors.dart';
import 'package:read_the_label/viewmodels/daily_intake_view_model.dart';
import 'package:read_the_label/views/widgets/daily_intake/macronutrient_indicator.dart';

class MacronutrientsIndicatorCard extends StatelessWidget {
  const MacronutrientsIndicatorCard({super.key});

  @override
  Widget build(BuildContext context) {
    debugPrint('REBUILD: MacronutrientsIndicatorCard');
    final proteinGoal = context.select(
      (DailyIntakeViewModel vm) => vm.proteinGoal,
    );
    final carbsGoal = context.select(
      (DailyIntakeViewModel vm) => vm.carbsGoal,
    );
    final fatGoal = context.select(
      (DailyIntakeViewModel vm) => vm.fatGoal,
    );

    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceAround,
      spacing: 10,
      children: [
        MacronutrientIndicator(
          label: 'Protein',
          nutrientName: AppConstants.protein,
          goal: proteinGoal,
          iconAsset: 'assets/icons/protein_icon.png',
          color: AppColors.limeGreen,
        ),
        MacronutrientIndicator(
          label: 'Carbs',
          nutrientName: AppConstants.totalCarbohydrate,
          goal: carbsGoal,
          iconAsset: 'assets/icons/carbs_icon.png',
          color: AppColors.yellow,
        ),
        MacronutrientIndicator(
          label: 'Fat',
          nutrientName: AppConstants.totalFat,
          goal: fatGoal,
          iconAsset: 'assets/icons/fat_icon.png',
          color: AppColors.amberOrange,
        ),
      ],
    );
  }
}
