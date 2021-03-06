package net.blay09.mods.fertilization;

import net.minecraft.block.*;
import net.minecraft.block.trees.Tree;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BoneMealHelper {

    private static Method getSeedMethod;

    @Nullable
    public static Item getSeedFromCrop(BlockState state) {
        if (state.getBlock() == Blocks.COCOA) {
            return Items.COCOA_BEANS;
        }

        if (getSeedMethod == null) {
            try {
                getSeedMethod = ObfuscationReflectionHelper.findMethod(CropsBlock.class, "func_199772_f");
            } catch (ObfuscationReflectionHelper.UnableToFindMethodException ignored) {
                return null;
            }

            getSeedMethod.setAccessible(true);
        }

        try {
            return (Item) getSeedMethod.invoke(state.getBlock());
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isGrassBlock(BlockState state) {
        return state.getBlock() == Blocks.GRASS_BLOCK;
    }

    @Nullable
    public static Tree getFancyTreeForSapling(BlockState state) {
        if (state.getBlock() == Blocks.OAK_SAPLING) {
            return new FancyTree(Blocks.OAK_LOG.getDefaultState(), Blocks.OAK_LEAVES.getDefaultState());
        } else if(state.getBlock() == Blocks.SPRUCE_SAPLING) {
            return new FancyTree(Blocks.SPRUCE_LOG.getDefaultState(), Blocks.SPRUCE_LEAVES.getDefaultState());
        } else if(state.getBlock() == Blocks.BIRCH_SAPLING) {
            return new FancyTree(Blocks.BIRCH_LOG.getDefaultState(), Blocks.BIRCH_LEAVES.getDefaultState());
        } else if(state.getBlock() == Blocks.JUNGLE_SAPLING) {
            return new FancyTree(Blocks.JUNGLE_LOG.getDefaultState(), Blocks.JUNGLE_LEAVES.getDefaultState());
        } else if(state.getBlock() == Blocks.ACACIA_SAPLING) {
            return new FancyTree(Blocks.ACACIA_LOG.getDefaultState(), Blocks.ACACIA_LEAVES.getDefaultState());
        } else if(state.getBlock() == Blocks.DARK_OAK_SAPLING) {
            return new FancyTree(Blocks.DARK_OAK_LOG.getDefaultState(), Blocks.DARK_OAK_LEAVES.getDefaultState());
        }

        return null;
    }

    public static boolean tryHarvest(@Nullable PlayerEntity player, World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (tryHarvestGeneric(player, world, pos, state, it -> it.getBlock() instanceof CropsBlock && ((CropsBlock) it.getBlock()).isMaxAge(it), () -> ((CropsBlock) state.getBlock()).withAge(0), 0.25f)) {
            return true;
        }

        //noinspection RedundantIfStatement
        if (tryHarvestGeneric(player, world, pos, state, it -> it.getBlock() == Blocks.COCOA && it.get(CocoaBlock.AGE) >= 2, Blocks.COCOA::getDefaultState, -0.75f)) {
            return true;
        }

        return false;
    }

    public static boolean tryHarvestGeneric(@Nullable PlayerEntity player, World world, BlockPos pos, BlockState state, Predicate<BlockState> isMature, Supplier<BlockState> newCropState, float spawnOffsetY) {
        if (!isMature.test(state)) {
            return false;
        }

        List<ItemStack> drops = world instanceof ServerWorld ? Block.getDrops(state, (ServerWorld) world, pos, null) : Collections.emptyList();

        Item seedItem = getSeedFromCrop(state);
        boolean foundSeed = false;
        for (ItemStack itemStack : drops) {
            if (!itemStack.isEmpty() && itemStack.getItem() == seedItem) {
                itemStack.shrink(1);
                foundSeed = true;
                break;
            }
        }

        ItemStack seedInInventory = player != null ? findSeedInInventory(player, seedItem) : ItemStack.EMPTY;
        if (!foundSeed && !seedInInventory.isEmpty()) {
            // If the crop did not drop a seed, try to find one in the player inventory and use that one instead.
            seedInInventory.shrink(1);
            foundSeed = true;
        }

        if (!foundSeed) {
            return false;
        }

        if (!world.isRemote) {
            world.setBlockState(pos, newCropState.get());

            for (ItemStack itemStack : drops) {
                if ((seedInInventory.isEmpty() && itemStack.getItem() == seedItem) || FertilizationConfig.COMMON.addDropsDirectlyToInventory.get() || (FertilizationConfig.COMMON.addDropsDirectlyToInventoryForFakePlayers.get() && player instanceof FakePlayer)) {
                    if (player != null && player.inventory.addItemStackToInventory(itemStack)) {
                        continue;
                    }
                }

                ItemEntity entityItem = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + spawnOffsetY, pos.getZ() + 0.5, itemStack);
                entityItem.setPickupDelay(10);
                world.addEntity(entityItem);
            }
        }

        return true;
    }

    private static ItemStack findSeedInInventory(PlayerEntity player, @Nullable Item seedItem) {
        for (ItemStack itemStack : player.inventory.mainInventory) {
            if (!itemStack.isEmpty() && itemStack.getItem() == seedItem) {
                return itemStack;
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean isStemCrop(BlockState state) {
        return state.getBlock() == Blocks.MELON_STEM || state.getBlock() == Blocks.PUMPKIN_STEM;
    }

    public static boolean isGrowableDisabledForCompressed(BlockState state) {
        return isGrassBlock(state) || state.getBlock() == Blocks.TALL_GRASS;
    }
}
