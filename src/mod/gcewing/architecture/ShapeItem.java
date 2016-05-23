//------------------------------------------------------------------------------
//
//   ArchitectureCraft - ShapeItem - Client
//
//------------------------------------------------------------------------------

package gcewing.architecture;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.*;
//import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.world.*;

import static gcewing.architecture.BaseBlockUtils.*;
import static gcewing.architecture.BaseUtils.*;
import static gcewing.architecture.Vector3.getDirectionVec;

public class ShapeItem extends BaseItemBlock {

	static Random rand = new Random();
	
	public ShapeItem(Block block) {
		super(block);
	}
	
	@Override
	public boolean placeBlockAt(ItemStack stack, EntityPlayer player, World world, BlockPos pos,
		EnumFacing face, float hitX, float hitY, float hitZ, IBlockState newState)
	{
		//if (!world.isRemote)
		//	System.out.printf("ShapeItem.placeBlockAt: hit = (%.3f, %.3f, %.3f)\n", hitX, hitY, hitZ);
		if (!setWorldBlockState(world, pos, newState, 3))
			return false;
		Vec3i d = getDirectionVec(face);
		Vector3 hit = new Vector3(hitX - d.getX() - 0.5, hitY - d.getY() - 0.5, hitZ - d.getZ() - 0.5);
		ShapeTE te = ShapeTE.get(world, pos);
		if (te != null) {
			te.readFromItemStack(stack);
			if (te.shape != null) {
				BlockPos npos = te.getPos().offset(oppositeFacing(face));
				IBlockState nstate = getWorldBlockState(world, npos);
				TileEntity nte = getWorldTileEntity(world, npos);
				te.shape.orientOnPlacement(player, te, npos, nstate, nte, face, hit);
			}
		}

		world.updateLightByType(EnumSkyBlock.Block, pos.getX(), pos.getY(), pos.getZ());
		return true;
	}
	
	@Override
	public boolean getShareTag() {
		return true;
	}

	@Override
	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean par4) {
		NBTTagCompound tag = stack.getTagCompound();
		if (tag != null) {
			int id = tag.getInteger("Shape");
			Shape shape = Shape.forId(id);
			if (shape != null)
				lines.set(0, shape.title);
			else
				lines.set(0, lines.get(0) + " (" + id + ")");
			Block baseBlock = Block.getBlockFromName(tag.getString("BaseName"));
			int baseMetadata = tag.getInteger("BaseData");
			if (baseBlock != null)
				lines.add(Utils.displayNameOfBlock(baseBlock, baseMetadata));
		}
	}

//	@Override
//	public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean par4) {
//		NBTTagCompound tag = stack.getTagCompound();
//		if (tag != null) {
//			int id = tag.getInteger("Shape");
//			Block baseBlock = Block.getBlockFromName(tag.getString("BaseName"));
//			int baseMetadata = tag.getInteger("BaseData");
//			Shape shape = Shape.forId(id);
//			if (shape != null)
//				lines.set(0, shape.title);
//			else
//				lines.set(0, lines.get(0) + " (" + id + ")");
//			if (baseBlock != null) {
//				String baseName = null;
//				Item baseItem = Item.getItemFromBlock(baseBlock);
//				if (baseItem != null) {
//					ItemStack baseStack = new ItemStack(baseItem, 1, baseMetadata);
//					baseName = baseStack.getDisplayName();
//				}
//				if (baseName == null)
//					baseName = baseBlock.getLocalizedName();
//				String desc = "Cut from " + baseName;
//				lines.add(desc);
//			}
//		}
//	}

}
