package cazador.furnaceoverhaul.blocks;

import java.util.List;
import java.util.Random;

import cazador.furnaceoverhaul.FurnaceOverhaul;
import cazador.furnaceoverhaul.Reference;
import cazador.furnaceoverhaul.handler.GuiHandler;
import cazador.furnaceoverhaul.init.ModBlocks;
import cazador.furnaceoverhaul.tile.TileEntityIronFurnace;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

public class IronFurnace extends BlockContainer{
	
	public static final PropertyDirection FACING = BlockHorizontal.FACING;
	public static final PropertyBool ACTIVE = PropertyBool.create("active");
	public static boolean keepInventory;
	
	public IronFurnace(String unlocalizedname) {
		super(Material.IRON);
		this.setUnlocalizedName(unlocalizedname);
        this.setRegistryName(new ResourceLocation(Reference.MOD_ID, unlocalizedname));
		setCreativeTab(FurnaceOverhaul.FurnaceOverhaulTab);
		this.setHardness(2.0F);
		this.setResistance(9.0F);
		this.setHarvestLevel("pickaxe", 1);
		setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(ACTIVE, false));
		
	}
	
	@Override
	public int getLightValue(IBlockState state, IBlockAccess world, BlockPos pos) {
		if (state.getValue(ACTIVE) == true){
			return 8;
		} else if (state.getValue(ACTIVE) == false);
			return 0;
	
	}
	
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World player, List<String> tooltip, ITooltipFlag advanced) {
		tooltip.add(TextFormatting.WHITE + "Cook time 160 ticks");
	}
	
	public static void setState(boolean active, World world, BlockPos pos){
        IBlockState iblockstate = world.getBlockState(pos);
        TileEntity te = world.getTileEntity(pos);
        keepInventory = true;
        
        if (active) {
            world.setBlockState(pos, ModBlocks.ironfurnace.getDefaultState().withProperty(FACING, iblockstate.getValue(FACING)).withProperty(ACTIVE, true), 3);
        }
        else {
            world.setBlockState(pos, ModBlocks.ironfurnace.getDefaultState().withProperty(FACING, iblockstate.getValue(FACING)).withProperty(ACTIVE, false), 3);
        }

        keepInventory = false;

        if (te != null){
            te.validate();
            world.setTileEntity(pos, te);
        }
    }
	
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityIronFurnace();
	}
	
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}
	
	public BlockRenderLayer getBlockRenderLayer() {
		return BlockRenderLayer.SOLID;
	}
	
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, new IProperty[] {FACING,ACTIVE});
	}

	public IBlockState getStateFromMeta(int meta) {
		EnumFacing enumfacing = EnumFacing.getFront(meta);
        if (enumfacing.getAxis() == EnumFacing.Axis.Y){
            enumfacing = EnumFacing.NORTH;
        }
        return this.getDefaultState().withProperty(FACING, enumfacing);
	}
	
	public int getMetaFromState(IBlockState state) {
		return ((EnumFacing)state.getValue(FACING)).getIndex();
	}
	
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
		return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}
		
	private void setDefaultFacing(World world, BlockPos pos, IBlockState state) {
		if (!world.isRemote){
            IBlockState state0 = world.getBlockState(pos.north());
            IBlockState state1 = world.getBlockState(pos.south());
            IBlockState state2 = world.getBlockState(pos.west());
            IBlockState state3 = world.getBlockState(pos.east());
            EnumFacing enumfacing = (EnumFacing)state.getValue(FACING);

            if (enumfacing == EnumFacing.NORTH && state0.isFullBlock() && !state1.isFullBlock()){
                enumfacing = EnumFacing.SOUTH;
            }
            else if (enumfacing == EnumFacing.SOUTH && state1.isFullBlock() && !state0.isFullBlock()){
                enumfacing = EnumFacing.NORTH;
            }
            else if (enumfacing == EnumFacing.WEST && state2.isFullBlock() && !state3.isFullBlock()){
                enumfacing = EnumFacing.EAST;
            }
            else if (enumfacing == EnumFacing.EAST && state3.isFullBlock() && !state2.isFullBlock()){
                enumfacing = EnumFacing.WEST;
            }
            world.setBlockState(pos, state.withProperty(FACING, enumfacing), 2);
        }
		
	}
	
	public IBlockState withRotation(IBlockState state, Rotation rot){
        return state.withProperty(FACING, rot.rotate((EnumFacing)state.getValue(FACING)));
    }

    public IBlockState withMirror(IBlockState state, Mirror mirror){
        return state.withRotation(mirror.toRotation((EnumFacing)state.getValue(FACING)));
    }
    
    public void onBlockAdded(World world, BlockPos pos, IBlockState state){
	    this.setDefaultFacing(world, pos, state);
	    }	
	
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

    public boolean hasComparatorInputOverride(IBlockState state){
        return true;
    }

    public int getComparatorInputOverride(IBlockState blockState, World worldIn, BlockPos pos){
        return Container.calcRedstone(worldIn.getTileEntity(pos));
    }
    
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ){
        if(!player.isSneaking() && !world.isRemote) {
        	player.openGui(FurnaceOverhaul.instance, GuiHandler.GUI_FURNACE, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
       }
	
	public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
		return new ItemStack(Item.getItemFromBlock(this), 1, (int) (getMetaFromState(world.getBlockState(pos)) / EnumFacing.values().length));
	}
	
	public void breakBlock(World world, BlockPos pos, IBlockState state){
    	if (!keepInventory){
    	TileEntity te = (TileEntityIronFurnace) world.getTileEntity(pos);
		IItemHandler handler = te.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		for(int slot = 0; slot < handler.getSlots(); slot++){
			ItemStack stack = handler.getStackInSlot(slot);
			InventoryHelper.dropInventoryItems(world, pos, (TileEntityIronFurnace)te);
            world.updateComparatorOutputLevel(pos, this);
			}
        }
        super.breakBlock(world, pos, state);
    }
	
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack){
        world.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
        if (stack.hasDisplayName()){
            TileEntity tileentity = world.getTileEntity(pos);
            if (tileentity instanceof TileEntityIronFurnace){
                ((TileEntityIronFurnace)tileentity).setCustomInventoryName(stack.getDisplayName());
            }
        }
    }    
	
}
