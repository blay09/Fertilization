package net.blay09.mods.fertilization.item;

import net.blay09.mods.fertilization.BoneMealHelper;
import net.blay09.mods.fertilization.Fertilization;
import net.blay09.mods.fertilization.FertilizationConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.IGrowable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;

public class CompressedBoneMealItem extends Item {

    public static final String name = "compressed_bonemeal";
    public static final ResourceLocation registryName = new ResourceLocation(Fertilization.MOD_ID, name);

    public CompressedBoneMealItem() {
        this(new Item.Properties());
    }

    public CompressedBoneMealItem(Item.Properties properties) {
        super(properties.group(Fertilization.itemGroup));
    }

    @Override
    public ActionResultType onItemUse(ItemUseContext useContext) {
        World world = useContext.getWorld();
        BlockPos pos = useContext.getPos();
        PlayerEntity player = useContext.getPlayer();
        BlockState state = world.getBlockState(pos);
        Hand hand = useContext.getHand();

        if (player == null) {
            return ActionResultType.PASS;
        }

        ActionResultType result = applyBoneMeal(world, pos, state, player.getHeldItem(hand), player);
        if (result == ActionResultType.FAIL) {
            player.swingArm(hand);
        } else if (result == ActionResultType.SUCCESS) {
            if (!player.abilities.isCreativeMode) {
                player.getHeldItem(hand).shrink(1);
            }
        }

        return result;
    }

    public ActionResultType applyBoneMeal(World world, BlockPos pos, BlockState state, ItemStack itemStack, @Nullable PlayerEntity player) {
        if (!(state.getBlock() instanceof IGrowable) || !((IGrowable) state.getBlock()).canUseBonemeal(world, world.rand, pos, state)) {
            return ActionResultType.PASS;
        }

        // Disable grass, no one would want to waste their hard-earned bone meal on that.
        if (BoneMealHelper.isGrowableDisabledForCompressed(state)) {
            return ActionResultType.FAIL;
        }

        boolean isStem = BoneMealHelper.isStemCrop(state);
        ItemStack boneMealStack = itemStack.copy();
        for (int i = 0; i < getBoneMealCount(); i++) {
            BoneMealHelper.tryHarvest(player, world, pos);

            boolean boneMealApplied = player != null ? BoneMealItem.applyBonemeal(boneMealStack, world, pos, player) : BoneMealItem.applyBonemeal(boneMealStack, world, pos);
            if (!boneMealApplied && !isStem) {
                break;
            }

            if (isStem && !world.isRemote) {
                state.tick((ServerWorld) world, pos, world.rand);
            }
        }

        return ActionResultType.SUCCESS;
    }

    protected int getBoneMealCount() {
        return FertilizationConfig.COMMON.compressedBoneMealPower.get();
    }

}
