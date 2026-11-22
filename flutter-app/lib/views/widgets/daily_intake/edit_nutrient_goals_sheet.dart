import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:read_the_label/theme/app_colors.dart';
import 'package:read_the_label/theme/app_text_styles.dart';
import 'package:read_the_label/viewmodels/daily_intake_view_model.dart';

class EditNutrientGoalsSheet extends StatefulWidget {
  const EditNutrientGoalsSheet({super.key});

  static Future<void> show(BuildContext context) {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => const EditNutrientGoalsSheet(),
    );
  }

  @override
  State<EditNutrientGoalsSheet> createState() => _EditNutrientGoalsSheetState();
}

class _EditNutrientGoalsSheetState extends State<EditNutrientGoalsSheet> {
  late TextEditingController _calorieController;
  late TextEditingController _proteinController;
  late TextEditingController _carbsController;
  late TextEditingController _fatController;

  @override
  void initState() {
    super.initState();
    final vm = context.read<DailyIntakeViewModel>();
    _calorieController = TextEditingController(text: vm.calorieGoal.toInt().toString());
    _proteinController = TextEditingController(text: vm.proteinGoal.toInt().toString());
    _carbsController = TextEditingController(text: vm.carbsGoal.toInt().toString());
    _fatController = TextEditingController(text: vm.fatGoal.toInt().toString());
  }

  @override
  void dispose() {
    _calorieController.dispose();
    _proteinController.dispose();
    _carbsController.dispose();
    _fatController.dispose();
    super.dispose();
  }

  Widget _buildField({
    required String label,
    required String unit,
    required TextEditingController controller,
    required Color accentColor,
    required String iconAsset,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: AppColors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Colors.white.withValues(alpha: 0.08),
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Image.asset(
            iconAsset,
            width: 20,
            height: 20,
            errorBuilder: (_, __, ___) => Icon(Icons.circle, color: accentColor, size: 18),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label.toUpperCase(),
                  style: AppTextStyles.caption.copyWith(
                    color: accentColor,
                    fontWeight: FontWeight.w700,
                    fontSize: 11,
                    letterSpacing: 0.8,
                  ),
                ),
                const SizedBox(height: 4),
                TextField(
                  controller: controller,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  style: AppTextStyles.bodyLargeBold.copyWith(
                    color: AppColors.primaryWhite,
                    fontSize: 18,
                  ),
                  decoration: InputDecoration(
                    isDense: true,
                    contentPadding: EdgeInsets.zero,
                    border: InputBorder.none,
                    hintText: '0',
                    hintStyle: TextStyle(color: Colors.grey.shade600),
                  ),
                ),
              ],
            ),
          ),
          Text(
            unit,
            style: AppTextStyles.bodyMedium.copyWith(
              color: AppColors.secondaryLabel,
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  void _saveGoals() async {
    final calories = double.tryParse(_calorieController.text.trim());
    final protein = double.tryParse(_proteinController.text.trim());
    final carbs = double.tryParse(_carbsController.text.trim());
    final fat = double.tryParse(_fatController.text.trim());

    final vm = context.read<DailyIntakeViewModel>();
    await vm.updateNutrientGoals(
      calories: calories,
      protein: protein,
      carbs: carbs,
      fat: fat,
    );

    if (mounted) {
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Nutrition goals updated successfully!'),
          backgroundColor: AppColors.limeGreen,
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  void _resetDefaults() async {
    final vm = context.read<DailyIntakeViewModel>();
    await vm.resetNutrientGoalsToDefaults();
    _calorieController.text = '2000';
    _proteinController.text = '50';
    _carbsController.text = '275';
    _fatController.text = '78';

    if (mounted) {
      Navigator.of(context).pop();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Goals reset to defaults (2000 kcal, 50g protein, 275g carbs, 78g fat)'),
          backgroundColor: AppColors.saturatedOrange,
          duration: Duration(seconds: 2),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;

    return Container(
      decoration: const BoxDecoration(
        color: AppColors.cardBackground,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: EdgeInsets.fromLTRB(20, 16, 20, 24 + bottomInset),
      child: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Center(
              child: Container(
                width: 40,
                height: 4,
                margin: const EdgeInsets.only(bottom: 16),
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.3),
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Daily Nutrition Goals',
                      style: AppTextStyles.heading2Bold.copyWith(
                        color: AppColors.primaryWhite,
                        fontSize: 20,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'Customize your daily target calories & macros',
                      style: AppTextStyles.bodySmall.copyWith(
                        color: AppColors.secondaryLabel,
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
                IconButton(
                  onPressed: () => Navigator.of(context).pop(),
                  icon: const Icon(Icons.close, color: AppColors.secondaryLabel),
                ),
              ],
            ),
            const SizedBox(height: 20),
            _buildField(
              label: 'Daily Calories',
              unit: 'kcal',
              controller: _calorieController,
              accentColor: AppColors.saturatedOrange,
              iconAsset: 'assets/icons/energy_icon.png',
            ),
            _buildField(
              label: 'Protein',
              unit: 'g',
              controller: _proteinController,
              accentColor: AppColors.limeGreen,
              iconAsset: 'assets/icons/protein_icon.png',
            ),
            _buildField(
              label: 'Carbohydrates',
              unit: 'g',
              controller: _carbsController,
              accentColor: AppColors.yellow,
              iconAsset: 'assets/icons/carbs_icon.png',
            ),
            _buildField(
              label: 'Fat',
              unit: 'g',
              controller: _fatController,
              accentColor: AppColors.amberOrange,
              iconAsset: 'assets/icons/fat_icon.png',
            ),
            const SizedBox(height: 12),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: _resetDefaults,
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.secondaryLabel,
                      side: BorderSide(color: Colors.white.withValues(alpha: 0.2)),
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                    ),
                    child: const Text('Reset Defaults'),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  flex: 2,
                  child: ElevatedButton(
                    onPressed: _saveGoals,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.limeGreen,
                      foregroundColor: Colors.black,
                      padding: const EdgeInsets.symmetric(vertical: 14),
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(14),
                      ),
                      elevation: 0,
                    ),
                    child: Text(
                      'Save Goals',
                      style: AppTextStyles.bodyMediumBold.copyWith(
                        color: Colors.black,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
