//------------------------------------------------------------------------------
//
//	 ArchitectureCraft - ShapeTE
//
//------------------------------------------------------------------------------

package gcewing.architecture;

import static java.lang.Math.*;

import net.minecraft.block.*;
import net.minecraft.block.state.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

//import static gcewing.architecture.BaseUtils.*;
import static gcewing.architecture.BaseBlockUtils.*;

public class ShapeTE extends BaseTileEntity {

	public Shape shape;
	public IBlockState baseBlockState;
	public IBlockState secondaryBlockState;
	public int disabledConnections;
	private byte offsetX;
	
	public static ShapeTE get(IBlockAccess world, BlockPos pos) {
		if (world != null) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof ShapeTE)
				return (ShapeTE)te;
		}
		return null;
	}
	
	public ShapeTE() {
		super();
		shape = Shape.RoofTile;
		baseBlockState = Blocks.planks.getDefaultState();
	}
	
	public ShapeTE(Shape s, IBlockState b) {
		super();
		shape = s;
		baseBlockState = b;
	}
	
	public ShapeTE(Shape s, Block b, int d) {
		super();
		shape = s;
		baseBlockState = b.getStateFromMeta(d);
	}
	
	double getOffsetX() {
		return offsetX * (1/16.0);
	}
	
	public void setOffsetX(double value) {
		offsetX = (byte)(16 * value);
	}
	
	@Override
	public Trans3 localToGlobalTransformation(Vector3 origin) {
		return super.localToGlobalTransformation(origin).translate(getOffsetX(), 0, 0);
	}

	public boolean connectionIsEnabledGlobal(EnumFacing dir) {
		return (disabledConnections & (1 << dir.ordinal())) == 0;
	}
	
	public void setConnectionEnabledGlobal(EnumFacing dir, boolean state) {
		int bit = 1 << dir.ordinal();
		if (state)
			disabledConnections &= ~bit;
		else
			disabledConnections |= bit;
		markChanged();
	}
	
	public void toggleConnectionGlobal(EnumFacing dir) {
		boolean newState = !connectionIsEnabledGlobal(dir);
		setConnectionEnabledGlobal(dir, newState);
		ShapeTE nte = getNeighbourGlobal(dir);
		if (nte != null)
			nte.setConnectionEnabledGlobal(dir.getOpposite(), newState);
	}

	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		readShapeFromNBT(nbt);
		readSecondaryMaterialFromNBT(nbt);
		offsetX = nbt.getByte("offsetX");
	}

	@Override	
	public void readFromItemStackNBT(NBTTagCompound nbt) {
		readShapeFromNBT(nbt);
	}

	protected void readShapeFromNBT(NBTTagCompound nbt) {
		shape = Shape.forId(nbt.getInteger("Shape"));
		baseBlockState = nbtGetBlockState(nbt, "BaseName", "BaseData");
		if (baseBlockState == null)
			baseBlockState = Blocks.planks.getDefaultState();
		disabledConnections = nbt.getInteger("Disconnected");
	}
	
	protected void readSecondaryMaterialFromNBT(NBTTagCompound nbt) {
		secondaryBlockState = nbtGetBlockState(nbt, "Name2", "Data2");
	}
	
	protected IBlockState nbtGetBlockState(NBTTagCompound nbt, String nameField, String dataField) {
		String blockName = nbt.getString(nameField);
		if (blockName != null && blockName.length() > 0) {
			Block block = Block.getBlockFromName(blockName);
			int data = nbt.getInteger(dataField);
			IBlockState state = block.getStateFromMeta(data);
			return state;
		}
		return null;
	}
		
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		writeShapeToNBT(nbt);
		writeSecondaryMaterialToNBT(nbt);
		if (offsetX != 0)
			nbt.setByte("offsetX", offsetX);
	}
	
	@Override	
	public void writeToItemStackNBT(NBTTagCompound nbt) {
		writeShapeToNBT(nbt);
	}
	
	protected void writeShapeToNBT(NBTTagCompound nbt) {
		if (shape != null) {
			nbt.setInteger("Shape", shape.id);
			nbtSetBlockState(nbt, "BaseName", "BaseData", baseBlockState);
		}
		if (disabledConnections != 0)
			nbt.setInteger("Disconnected", disabledConnections);
	}
	
	protected void writeSecondaryMaterialToNBT(NBTTagCompound nbt) {
		nbtSetBlockState(nbt, "Name2", "Data2", secondaryBlockState);
	}
	
	protected void nbtSetBlockState(NBTTagCompound nbt, String nameField, String dataField, IBlockState state) {
		if (state != null) {
			Block block = state.getBlock();
			nbt.setString(nameField, getNameForBlock(block));
			nbt.setInteger(dataField, block.getMetaFromState(state));
		}
	}
		
	public void onChiselUse(EntityPlayer player, EnumFacing face, float hitX, float hitY, float hitZ) {
		shape.kind.onChiselUse(this, player, face, hitVec(hitX, hitY, hitZ));
	}

	public void onHammerUse(EntityPlayer player, EnumFacing face, float hitX, float hitY, float hitZ) {
		shape.kind.onHammerUse(this, player, face, hitVec(hitX, hitY, hitZ));
	}
	
	protected Vector3 hitVec(float hitX, float hitY, float hitZ) {
		return new Vector3(hitX - 0.5, hitY - 0.5, hitZ - 0.5);
	}
	
	public EnumFacing globalFace(EnumFacing face) {
		return localToGlobalRotation().t(face);
	}

	public EnumFacing localFace(EnumFacing face) {
		return localToGlobalRotation().it(face);
	}

	public boolean applySecondaryMaterial(ItemStack stack, EntityPlayer player) {
		IBlockState materialState = null;
		Item item = stack.getItem();
		if (item instanceof CladdingItem && shape.kind.acceptsCladding())
			materialState = ((CladdingItem)item).blockStateFromStack(stack);
		else {
			Block block = Block.getBlockFromItem(item);
			if (block != null) {
				IBlockState state = block.getStateFromMeta(stack.getMetadata());
				if (shape.kind.isValidSecondaryMaterial(state)) {
					materialState = state;
				}
			}
		}
		if (materialState != null) {
			if (secondaryBlockState == null) {
				setSecondaryMaterial(materialState);
				if (!Utils.playerIsInCreativeMode(player))
					--stack.stackSize;
			}
			getWorld().checkLight(getPos());
			return true;
		}
		else
			return false;
	}
	
	public void setSecondaryMaterial(IBlockState state) {
		secondaryBlockState = state;
		markChanged();
	}
	
	public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
		if (baseBlockState.getBlock().canRenderInLayer(layer))
			return true;
		if (secondaryBlockState != null)
			return secondaryBlockState.getBlock().canRenderInLayer(layer);
		return false;
	}
	
	public ShapeTE getNeighbourGlobal(EnumFacing dir) {
		return ShapeTE.get(worldObj, pos.offset(dir));
	}
	
	public ShapeTE getConnectedNeighbourGlobal(EnumFacing dir) {
		if (worldObj != null) {
			if (connectionIsEnabledGlobal(dir)) {
				ShapeTE nte = getNeighbourGlobal(dir);
				if (nte != null && nte.connectionIsEnabledGlobal(dir.getOpposite()))
					return nte;
			}
		}
		return null;
	}
	
}
