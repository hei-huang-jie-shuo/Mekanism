package mekanism.common.content.gear.mekatool;

import mekanism.api.text.IHasTextComponent;
import mekanism.common.MekanismLang;
import mekanism.common.content.gear.Module;
import mekanism.common.content.gear.ModuleConfigItem;
import mekanism.common.content.gear.ModuleConfigItem.BooleanData;
import mekanism.common.content.gear.ModuleConfigItem.EnumData;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ModuleMekaTool extends Module {

    public ActionResultType onItemUse(ItemUseContext context) {
        return ActionResultType.PASS;
    }

    public static class ModuleAttackAmplificationUnit extends ModuleMekaTool {
        private ModuleConfigItem<AttackDamage> attackDamage;

        @Override
        public void init() {
            super.init();
            addConfigItem(attackDamage = new ModuleConfigItem<AttackDamage>(this, "attack_damage", MekanismLang.MODULE_ATTACK_DAMAGE, new EnumData<>(AttackDamage.class, getInstalledCount()+2), AttackDamage.MED));
        }

        public int getDamage() {
            return attackDamage.get().getDamage();
        }

        public static enum AttackDamage implements IHasTextComponent {
            OFF(0),
            LOW(4),
            MED(8),
            HIGH(16),
            MAX(32);
            private int damage;
            private ITextComponent label;
            private AttackDamage(int damage) {
                this.damage = damage;
                this.label = new StringTextComponent(Integer.toString(damage));
            }
            @Override
            public ITextComponent getTextComponent() {
                return label;
            }
            public int getDamage() {
                return damage;
            }
        }
    }

    public static class ModuleSilkTouchUnit extends ModuleMekaTool {}

    public static class ModuleVeinMiningUnit extends ModuleMekaTool {
        private ModuleConfigItem<Boolean> extendedMode;

        @Override
        public void init() {
            super.init();
            addConfigItem(extendedMode = new ModuleConfigItem<Boolean>(this, "extended_mode", MekanismLang.MODULE_EXTENDED_MODE, new BooleanData(), false));
        }

        public boolean isExtended() {
            return extendedMode.get();
        }
    }

    public static class ModuleTeleportationUnit extends ModuleMekaTool {}
}