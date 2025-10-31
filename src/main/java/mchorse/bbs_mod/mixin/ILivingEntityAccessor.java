package mchorse.bbs_mod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import net.minecraft.entity.LivingEntity;

@Mixin(LivingEntity.class)
public interface ILivingEntityAccessor
{
    @Accessor("itemUseTimeLeft")
    int getItemUseTimeLeft();

    @Accessor("itemUseTimeLeft")
    void setItemUseTimeLeft(int value);
}

