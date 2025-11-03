package mchorse.bbs_mod.actions.types.item;

import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.items.GunItem;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;

public class UseItemActionClip extends ItemActionClip
{
    // Duration to hold the item use (for bows, food, etc.)
    public final ValueInt useDuration = new ValueInt("use_duration", 0);

    public UseItemActionClip()
    {
        super();
        this.add(this.useDuration);
    }

    @Override
    public void applyAction(LivingEntity actor, SuperFakePlayer player, Film film, Replay replay, int tick)
    {
        Hand hand = this.hand.get() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        ItemStack itemToUse = this.itemStack.get().copy();
        
        System.out.println("BBS MOD [ANIMATION PLAYBACK]: Applying item use at tick " + tick + 
                           " - " + itemToUse.getItem().getName().getString() + 
                           " in " + (hand == Hand.MAIN_HAND ? "MAIN" : "OFF") + " hand" +
                           " | Actor type: " + (actor != null ? actor.getClass().getSimpleName() : "null"));

        GunItem.actor = actor;

        // For first-person mode (when actor is a real ServerPlayerEntity):
        // - Bows and similar charge-up items are handled by keyframes in ActionPlayer
        // - Instant-use items (guns, throwables, etc.) still need action clips
        if (actor instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer)
        {
            // Skip bows and crossbows - they're handled by keyframes for proper draw animation
            if (itemToUse.getItem() instanceof net.minecraft.item.BowItem || 
                itemToUse.getItem() instanceof net.minecraft.item.CrossbowItem)
            {
                System.out.println("BBS MOD [ANIMATION PLAYBACK]:   Skipping bow/crossbow UseItemActionClip (handled by keyframes)");
                return;
            }
            
            // For other items (guns, snowballs, etc.), apply instantly
            System.out.println("BBS MOD [ANIMATION PLAYBACK]:   Using actor directly (first-person mode)");
            
            ItemStack originalStack = serverPlayer.getStackInHand(hand).copy();
            serverPlayer.setStackInHand(hand, itemToUse);
            
            // Call use() for instant-use items
            net.minecraft.util.TypedActionResult<ItemStack> result = itemToUse.use(serverPlayer.getWorld(), serverPlayer, hand);
            System.out.println("BBS MOD [ANIMATION PLAYBACK]:   Item use result: " + result.getResult());
            
            return;
        }
        else if (actor != null)
        {
            // For third-person mode (ActorEntity), use the fake player as before
            System.out.println("BBS MOD [ANIMATION PLAYBACK]:   Using fake player (third-person mode)");
            this.applyPositionRotation(player, replay, tick);
            player.setStackInHand(hand, itemToUse);
            itemToUse.use(player.getWorld(), player, hand);
            player.setStackInHand(hand, ItemStack.EMPTY);
        }
        else
        {
            System.out.println("BBS MOD [ANIMATION PLAYBACK]:   ERROR - Actor is null! Using fake player as fallback");
            this.applyPositionRotation(player, replay, tick);
            player.setStackInHand(hand, itemToUse);
            itemToUse.use(player.getWorld(), player, hand);
            player.setStackInHand(hand, ItemStack.EMPTY);
        }

        GunItem.actor = null;
    }

    @Override
    public boolean isClient()
    {
        return true;
    }

    @Override
    protected void applyClientAction(mchorse.bbs_mod.forms.entities.IEntity entity, Film film, Replay replay, int tick)
    {
        // Trigger hand swing animation on client side for visual feedback
        entity.swingArm();
        
        System.out.println("BBS MOD [ANIMATION PLAYBACK]: Client-side item use animation triggered");
    }

    @Override
    protected Clip create()
    {
        return new UseItemActionClip();
    }
}