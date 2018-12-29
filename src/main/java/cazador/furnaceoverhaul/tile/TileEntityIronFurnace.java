package cazador.furnaceoverhaul.tile;

import cazador.furnaceoverhaul.api.IUpgrade;
import cazador.furnaceoverhaul.blocks.DiamondFurnace;
import cazador.furnaceoverhaul.blocks.IronFurnace;
import cazador.furnaceoverhaul.capability.EnergyStorageFurnace;
import cazador.furnaceoverhaul.init.ModItems;
import cazador.furnaceoverhaul.inventory.ContainerFO;
import cazador.furnaceoverhaul.utils.OreDoublingRegistry;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.SlotFurnaceFuel;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.datafix.DataFixer;
import net.minecraft.util.datafix.FixTypes;
import net.minecraft.util.datafix.walkers.ItemStackDataLists;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.SidedInvWrapper;

public class TileEntityIronFurnace extends TileEntityLockable implements ITickable, ISidedInventory, IUpgrade {
		
	//input slot
	public static final int[] SLOTS_INPUT = {0};
	//output slot and fuel slot
    public static final int[] SLOTS_BOTTOM = {2, 1};
    //fuel slot
    public static final int[] SLOTS_FUEL = {1};
    //upgrade slots
    public static final int[] SLOT_UPGRADE = new int[] {3, 4, 5};
    public NonNullList<ItemStack> slot = NonNullList.<ItemStack>withSize(6, ItemStack.EMPTY);
    public int furnaceBurnTime;
    public int currentItemBurnTime;
    public int cookTime;
    public int totalCookTime;
    public String furnaceCustomName;
    private EnergyStorageFurnace storage = new EnergyStorageFurnace(100000, FE_PER_TICK_INPUT);
    public static final int FE_PER_TICK_INPUT = 500;
    public static final int FE_PER_TICK = 500;
    
    public boolean hasUpgrade(Item item) {
    	for (int slotId : SLOT_UPGRADE) {
    		final ItemStack stack = this.getStackInSlot(slotId);
    		if (stack.getItem() == item) {
    			return true;
    		}
    	}
    		
    	return false;
    }

    public void update(){
        boolean flag = this.isBurning();
        boolean flag1 = false;
        
        if (!this.world.isRemote){
        	if(hasUpgrade(ModItems.electricfuel) == true) {
        		if (this.storage.getEnergyStored() < FE_PER_TICK) {
        			IronFurnace.setState(false, this.world, this.pos);
        			return;
        		}
        		else if(hasUpgrade(ModItems.electricfuel) == true && this.cookTime > 0) {
        			
        			if (this.isBurning() && this.canSmelt() && cookTime == this.getCookTime((ItemStack)this.slot.get(0))) {
        				IronFurnace.setState(true, this.world, this.pos);
        				cookTime++; 
                        storage.consumePower(FE_PER_TICK);
                    }
                    markDirty();
                } else {
                    smeltItem();
                    IronFurnace.setState(this.isBurning(), this.world, this.pos);
                }
        }
        
        
        else if (this.isBurning() && hasUpgrade(ModItems.electricfuel) == false){
            --this.furnaceBurnTime;
        }

        if (!this.world.isRemote){
            ItemStack itemstack = (ItemStack)this.slot.get(1);

            if (this.isBurning() || !itemstack.isEmpty() && !((ItemStack)this.slot.get(0)).isEmpty()){
                if (!this.isBurning() && this.canSmelt()){
                    this.furnaceBurnTime = getItemBurnTime(itemstack);
                    this.currentItemBurnTime = this.furnaceBurnTime;

                    if (this.isBurning()){
                        flag1 = true;

                        if (!itemstack.isEmpty()){
                            Item item = itemstack.getItem();
                            itemstack.shrink(1);

                            if (itemstack.isEmpty()){
                                ItemStack item1 = item.getContainerItem(itemstack);
                                this.slot.set(1, item1);
                            }
                        }
                    }
                }

                if (this.isBurning() && this.canSmelt()){
                    ++this.cookTime;

                    if (this.cookTime == this.totalCookTime){
                        this.cookTime = 0;
                        this.totalCookTime = this.getCookTime((ItemStack)this.slot.get(0));
                        this.smeltItem();
                        flag1 = true;
                    }
                }
                else{
                    this.cookTime = 0;
                }
            }
            else if (!this.isBurning() && this.cookTime > 0){
                this.cookTime = MathHelper.clamp(this.cookTime - 2, 0, this.totalCookTime);
            }

            if (flag != this.isBurning()){
                flag1 = true;
                IronFurnace.setState(this.isBurning(), this.world, this.pos);
            }
        }

        if (flag1){
            this.markDirty();
        }
    }
}

    
    public NBTTagCompound writeToNBT(NBTTagCompound compound){
        super.writeToNBT(compound);
        compound.setInteger("BurnTime", (short)this.furnaceBurnTime);
        compound.setInteger("CookTime", (short)this.cookTime);
        compound.setInteger("CookTimeTotal", (short)this.totalCookTime);
        compound.setInteger("energy", this.storage.getEnergyStored());
        ItemStackHelper.saveAllItems(compound, this.slot);
        if (this.hasCustomName()){
            compound.setString("CustomName", this.furnaceCustomName);
        }
        return compound;
    }
    
    public void readFromNBT(NBTTagCompound compound){
        super.readFromNBT(compound);
        this.slot = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(compound, this.slot);
        this.furnaceBurnTime = compound.getInteger("BurnTime");
        this.cookTime = compound.getInteger("CookTime");
        this.totalCookTime = compound.getInteger("CookTimeTotal");
        this.currentItemBurnTime = getItemBurnTime((ItemStack)this.slot.get(1));
        this.storage.setEnergy(compound.getInteger("energy"));
        if (compound.hasKey("CustomName", 8)){
            this.furnaceCustomName = compound.getString("CustomName");
        }
    }
    
    public int getEnergy() {
        return this.storage.getEnergyStored();
    }
    
    public int getMaxEnergyStored() {
    	return this.storage.getMaxEnergyStored();
    }

    public int getCookTime(ItemStack stack){
        if(hasUpgrade(ModItems.speed) == true) {
        	return 140;
        } else
        return 170;
    }

    public boolean canSmelt() {
        if (((ItemStack)this.slot.get(0)).isEmpty()){
            return false;
        } else {
            ItemStack itemstack = FurnaceRecipes.instance().getSmeltingResult((ItemStack)this.slot.get(0));
            if (itemstack.isEmpty()){
                return false;
            } else {
                ItemStack itemstack1 = (ItemStack)this.slot.get(2);
                if (itemstack1.isEmpty()) return true;
                if (!itemstack1.isItemEqual(itemstack)) return false;
                int result = itemstack1.getCount() + itemstack.getCount();
                return result <= getInventoryStackLimit() && result <= itemstack1.getMaxStackSize();
            }
        }
    }

    public void smeltItem() {
        if (this.canSmelt()) {
            ItemStack input = this.slot.get(0);
            ItemStack recipeResult = FurnaceRecipes.instance().getSmeltingResult(input);
            ItemStack output = this.slot.get(2);

            if (output.isEmpty()) {
                ItemStack returnStack = recipeResult.copy();
                if (hasUpgrade(ModItems.oreprocessing) == true) {
                	if(OreDoublingRegistry.getSmeltingResult(input).getCount()==1){
                		 returnStack.setCount(returnStack.getCount());
                	} else{
                		 returnStack.setCount(returnStack.getCount() * 2);
                	}
                }
                slot.set(2, returnStack);
            } else if (output.getItem() == recipeResult.getItem() && hasUpgrade(ModItems.oreprocessing) == true){
            	if(OreDoublingRegistry.getSmeltingResult(input).getCount()==1){
            		output.grow(1);
            	} else{
            		output.grow(2);
            	}
            } else if (output.getItem() == recipeResult.getItem()) {
            	output.grow(recipeResult.getCount());
            }
            if (input.getItem() == Item.getItemFromBlock(Blocks.SPONGE) && input.getMetadata() == 1 && !((ItemStack)this.slot.get(1)).isEmpty() && ((ItemStack)this.slot.get(1)).getItem() == Items.BUCKET) {
                this.slot.set(1, new ItemStack(Items.WATER_BUCKET));
            }
            input.shrink(1);
        }
    }
    
	public int getItemBurnTime(ItemStack stack) {
		if (hasUpgrade(ModItems.electricfuel) == true) {
			return 0;
		}
		else if (hasUpgrade(ModItems.efficiency) == true) {
			return TileEntityFurnace.getItemBurnTime(stack) * 2;
		}
			return TileEntityFurnace.getItemBurnTime(stack);
	}

	public static boolean isItemFuel(ItemStack stack){
        return TileEntityFurnace.getItemBurnTime(stack) > 0;
    }
	
	public int getSizeInventory(){
        return this.slot.size();
    }

    public boolean isEmpty(){
        for (ItemStack itemstack : this.slot){
            if (!itemstack.isEmpty()){
                return false;
            }
        }

        return true;
    }

    public ItemStack getStackInSlot(int index){
        return (ItemStack)this.slot.get(index);
    }

    public ItemStack decrStackSize(int index, int count){
        return ItemStackHelper.getAndSplit(this.slot, index, count);
    }

    public ItemStack removeStackFromSlot(int index){
        return ItemStackHelper.getAndRemove(this.slot, index);
    }

    public void setInventorySlotContents(int index, ItemStack stack){
        ItemStack itemstack = (ItemStack)this.slot.get(index);
        boolean flag = !stack.isEmpty() && stack.isItemEqual(itemstack) && ItemStack.areItemStackTagsEqual(stack, itemstack);
        this.slot.set(index, stack);

        if (stack.getCount() > this.getInventoryStackLimit()){
            stack.setCount(this.getInventoryStackLimit());
        }

        if (index == 0 && !flag){
            this.totalCookTime = this.getCookTime(stack);
            this.cookTime = 0;
            this.markDirty();
        }
    }

    public String getName(){
        return this.hasCustomName() ? this.furnaceCustomName : "container.furnace";
    }

    public boolean hasCustomName(){
        return this.furnaceCustomName != null && !this.furnaceCustomName.isEmpty();
    }

    public void setCustomInventoryName(String p_145951_1_){
        this.furnaceCustomName = p_145951_1_;
    }

    public static void registerFixesFurnace(DataFixer fixer){
        fixer.registerWalker(FixTypes.BLOCK_ENTITY, new ItemStackDataLists(TileEntityIronFurnace.class, new String[] {"Items"}));
    }

    public int getInventoryStackLimit(){
        return 64;
    }

    public boolean isBurning(){
        return this.furnaceBurnTime > 0;
    }

    @SideOnly(Side.CLIENT)
    public static boolean isBurning(IInventory inventory){
        return inventory.getField(0) > 0;
    }
    
    public boolean isUsableByPlayer(EntityPlayer player){
        return this.world.getTileEntity(this.pos) != this ? false : player.getDistanceSq((double)this.pos.getX() + 0.5D, (double)this.pos.getY() + 0.5D, (double)this.pos.getZ() + 0.5D) <= 64.0D;
    }

    public void openInventory(EntityPlayer player){}

    public void closeInventory(EntityPlayer player){}

    public boolean isItemValidForSlot(int index, ItemStack stack){
        if (index == 2){
            return false;
        } else if (index != 1){
            return true;
        } else{
            ItemStack itemstack = (ItemStack)this.slot.get(1);
            return isItemFuel(stack) || SlotFurnaceFuel.isBucket(stack) && itemstack.getItem() != Items.BUCKET;
        }
    }

    public int[] getSlotsForFace(EnumFacing side){
    	 if (side == EnumFacing.DOWN){
             return SLOTS_BOTTOM;
         } else {
             return side == EnumFacing.UP ? SLOTS_INPUT : SLOTS_FUEL;
         }
    }

    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction){
        return this.isItemValidForSlot(index, itemStackIn);
    }

    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction){
        if (direction == EnumFacing.DOWN && index == 1){
            Item item = stack.getItem();
            if (item != Items.WATER_BUCKET && item != Items.BUCKET){
                return false;
            }
        }
        return true;
    }

    public String getGuiID(){
        return "furnaceoverhaul:ironfurnace";
    }

    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn){
        return new ContainerFO(playerInventory, this);
    }

    public int getField(int id){
        switch (id){
            case 0:
                return this.furnaceBurnTime;
            case 1:
                return this.currentItemBurnTime;
            case 2:
                return this.cookTime;
            case 3:
                return this.totalCookTime;
            default:
                return 0;
        }
    }

    public void setField(int id, int value){
        switch (id){
            case 0:
                this.furnaceBurnTime = value;
                break;
            case 1:
                this.currentItemBurnTime = value;
                break;
            case 2:
                this.cookTime = value;
                break;
            case 3:
                this.totalCookTime = value;
        }
    }

    public int getFieldCount(){
        return 4;
    }

    public void clear(){
        this.slot.clear();
    }
    
    @Override
	public SPacketUpdateTileEntity getUpdatePacket() {
		NBTTagCompound nbt = new NBTTagCompound();
		this.writeToNBT(nbt);
		int metadata = getBlockMetadata();
		return new SPacketUpdateTileEntity(this.pos, metadata, nbt);
	}
    
	public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
		if(capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) return true;
		if(capability == CapabilityEnergy.ENERGY) return true;
		else return false;

	}
    
    public <T> T getCapability(Capability<T> capability, EnumFacing facing){
    	if(capability == CapabilityEnergy.ENERGY)
			return (T) this.storage;
    	if (facing != null && capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY){
            if (facing == EnumFacing.DOWN)
                return (T) new SidedInvWrapper(this, EnumFacing.DOWN);
            else if (facing == EnumFacing.UP)
                return (T) new SidedInvWrapper(this, EnumFacing.UP);
            else
                return (T) new SidedInvWrapper(this, EnumFacing.WEST);
    }
    return super.getCapability(capability, facing);
    }
    
}