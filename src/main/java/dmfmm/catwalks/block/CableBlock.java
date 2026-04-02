package dmfmm.catwalks.block;

import dmfmm.catwalks.registry.BlockRegistry;
import dmfmm.catwalks.registry.ItemRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;


public class CableBlock extends GenericBlock {

    public enum CableFacing implements IStringSerializable {
        EAST, WEST, NORTH, SOUTH, NONE;

        @Override
        public String getName() {
            return this.name().toLowerCase();
        }

        public EnumFacing toEnumFacing() {
            switch (this) {
                case EAST:  return EnumFacing.EAST;
                case WEST:  return EnumFacing.WEST;
                case NORTH: return EnumFacing.NORTH;
                case SOUTH: return EnumFacing.SOUTH;
                default:    return null;
            }
        }

        public static CableFacing fromEnumFacing(EnumFacing f) {
            switch (f) {
                case EAST:  return EAST;
                case WEST:  return WEST;
                case NORTH: return NORTH;
                case SOUTH: return SOUTH;
                default:    return NONE;
            }
        }
    }

    public static final PropertyEnum<CableFacing> PREFERRED_FACING =
            PropertyEnum.create("preferred_facing", CableFacing.class);
    private static final CableFacing[] CYCLE = {
            CableFacing.EAST, CableFacing.WEST, CableFacing.NORTH, CableFacing.SOUTH, CableFacing.NONE
    };
    private static final EnumFacing[] HORIZONTALS = {
            EnumFacing.EAST, EnumFacing.WEST, EnumFacing.NORTH, EnumFacing.SOUTH
    };

    public CableBlock() {
        super("cable");
        this.setDefaultState(this.blockState.getBaseState()
                .withProperty(PREFERRED_FACING, CableFacing.EAST));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
                                    EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() != ItemRegistry.BLOW_TORCH) {
            return false;
        }
        List<CableFacing> validFacings = new ArrayList<>();
        for (EnumFacing dir : HORIZONTALS) {
            if (world.getBlockState(pos.offset(dir)).getBlock() == BlockRegistry.CATWALK) {
                validFacings.add(CableFacing.fromEnumFacing(dir));
            }
        }
        validFacings.add(CableFacing.NONE);

        if (validFacings.size() < 2) {
            return false;
        }

        if (!world.isRemote) {
            CableFacing current = state.getValue(PREFERRED_FACING);
            int idx = validFacings.indexOf(current);
            CableFacing next;
            if (idx == -1) {
                next = validFacings.get(0);
            } else {
                next = validFacings.get((idx + 1) % validFacings.size());
            }
            world.setBlockState(pos, state.withProperty(PREFERRED_FACING, next), 3);
        }
        return true;
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos)
    {
        if (state instanceof IExtendedBlockState) {
            boolean matchup   = world.getBlockState(pos.up()).getBlock() == this;
            boolean matchdown = world.getBlockState(pos.down()).getBlock() == this;

            IExtendedBlockState theState = (IExtendedBlockState) state;

            if (matchup && matchdown) {
                theState = theState.withProperty(STATE, CableState.MIDDLE);
            } else if (matchup) {
                theState = theState.withProperty(STATE, CableState.BOTTOM);
            } else {
                theState = theState.withProperty(STATE, CableState.TOP);
            }

            CableFacing preferred = state.getValue(PREFERRED_FACING);

            if (preferred == CableFacing.NONE) {
                theState = theState.withProperty(CONNECTED, false);
                return theState;
            }

            EnumFacing preferredDir = preferred.toEnumFacing();
            if (world.getBlockState(pos.offset(preferredDir)).getBlock() == BlockRegistry.CATWALK) {
                theState = theState.withProperty(FACING, preferredDir);
                theState = theState.withProperty(CONNECTED, true);
                return theState;
            }

            for (EnumFacing dir : HORIZONTALS) {
                if (world.getBlockState(pos.offset(dir)).getBlock() == BlockRegistry.CATWALK) {
                    theState = theState.withProperty(FACING, dir);
                    theState = theState.withProperty(CONNECTED, true);
                    return theState;
                }
            }

            theState = theState.withProperty(CONNECTED, false);
            return theState;
        }

        return state;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        switch (state.getValue(PREFERRED_FACING)) {
            case EAST:  return 0;
            case WEST:  return 1;
            case NORTH: return 2;
            case SOUTH: return 3;
            case NONE:  return 4;
            default:    return 0;
        }
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        switch (meta) {
            case 1:  return getDefaultState().withProperty(PREFERRED_FACING, CableFacing.WEST);
            case 2:  return getDefaultState().withProperty(PREFERRED_FACING, CableFacing.NORTH);
            case 3:  return getDefaultState().withProperty(PREFERRED_FACING, CableFacing.SOUTH);
            case 4:  return getDefaultState().withProperty(PREFERRED_FACING, CableFacing.NONE);
            default: return getDefaultState().withProperty(PREFERRED_FACING, CableFacing.EAST);
        }
    }

    @Deprecated
    public void addCollisionBoxToList(IBlockState state, World worldIn, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn, boolean isActualState)
    {
        addCollisionBoxToList(pos, entityBox, collidingBoxes, this.getBoundingBox(state, worldIn, pos));
    }

    public static final AxisAlignedBB CABLE_BOX            = new AxisAlignedBB(0.44, 0, 0.44, 0.56, 1, 0.56);
    public static final AxisAlignedBB CLIP_BOX_NORTH_SOUTH = new AxisAlignedBB(0.23, 0, 0, 0.75, 0.13, 0.59);
    public static final AxisAlignedBB CLIP_BOX_EAST_WEST   = new AxisAlignedBB(0, 0, 0.23, 0.59, 0.13, 0.75);

    @Deprecated
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos)
    {
        IBlockState estate = this.getExtendedState(state, source, pos);
        if (estate instanceof IExtendedBlockState) {
            IExtendedBlockState estater = (IExtendedBlockState) estate;
            if (estater.getValue(CONNECTED)) {
                switch (estater.getValue(FACING)) {
                    case NORTH: return CABLE_BOX.union(CLIP_BOX_NORTH_SOUTH);
                    case SOUTH: return CABLE_BOX.union(CLIP_BOX_NORTH_SOUTH).offset(0, 0, 0.4);
                    case EAST:  return CABLE_BOX.union(CLIP_BOX_EAST_WEST).offset(0.4, 0, 0);
                    case WEST:  return CABLE_BOX.union(CLIP_BOX_EAST_WEST);
                }
            } else {
                return CABLE_BOX;
            }
        }
        return CABLE_BOX;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) { return false; }

    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) { return false; }

    @Deprecated
    public boolean isFullCube(IBlockState state) { return false; }

    @Override
    public BlockStateContainer createBlockState() {
        return new BlockStateContainer.Builder(this)
                .add(PREFERRED_FACING)
                .add(CONNECTED).add(FACING).add(STATE)
                .build();
    }

    public enum CableState implements IStringSerializable {
        TOP, MIDDLE, BOTTOM;

        @Override
        public String getName() {
            return this.toString().toLowerCase();
        }
    }

    public static final IUnlistedProperty<CableState> STATE = new IUnlistedProperty<CableState>() {
        @Override public String getName() { return "cablestate"; }
        @Override public boolean isValid(CableState value) { return true; }
        @Override public Class<CableState> getType() { return CableState.class; }
        @Override public String valueToString(CableState value) { return value.toString().toLowerCase(); }
    };

    public static final IUnlistedProperty<Boolean> CONNECTED = new IUnlistedProperty<Boolean>() {
        @Override public String getName() { return "connected"; }
        @Override public boolean isValid(Boolean value) { return true; }
        @Override public Class<Boolean> getType() { return Boolean.class; }
        @Override public String valueToString(Boolean value) { return value ? "true" : "false"; }
    };

    public static final IUnlistedProperty<EnumFacing> FACING = new IUnlistedProperty<EnumFacing>() {
        @Override public String getName() { return "direction"; }
        @Override public boolean isValid(EnumFacing value) { return value != EnumFacing.UP && value != EnumFacing.DOWN; }
        @Override public Class<EnumFacing> getType() { return EnumFacing.class; }
        @Override public String valueToString(EnumFacing value) { return value.toString().toLowerCase(); }
    };
}
