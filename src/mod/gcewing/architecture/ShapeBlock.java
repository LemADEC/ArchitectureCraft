//------------------------------------------------------------------------------
//
//   ArchitectureCraft - ShapeBlock
//
//------------------------------------------------------------------------------

package gcewing.architecture;

import java.util.*;

import net.minecraft.block.*;
import net.minecraft.block.material.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.*;
import net.minecraft.init.*;
import net.minecraft.item.*;
import net.minecraft.tileentity.*;
import net.minecraft.world.*;
import net.minecraft.util.*;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fml.relauncher.*;

import static gcewing.architecture.Shape.*;

public class ShapeBlock extends BaseBlock<ShapeTE> {

	protected AxisAlignedBB boxHit;

	public ShapeBlock() {
		super(Material.rock, ShapeTE.class);
		//renderID = -1;
	}
	
	@Override
	public IOrientationHandler getOrientationHandler() {
		return BaseOrientation.orient24WaysByTE;
	}

	@Override
	public boolean isFullCube() {
		return false;
	}
	
	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public MovingObjectPosition collisionRayTrace(World world, BlockPos pos, Vec3 start, Vec3 end) {
		MovingObjectPosition result = null;
		double nearestDistance = 0;
		IBlockState state = world.getBlockState(pos);
		List<AxisAlignedBB> list = getGlobalCollisionBoxes(world, pos, state, null);
		if (list != null) {
			int n = list.size();
			for (int i = 0; i < n; i++) {
				AxisAlignedBB box = list.get(i);
				MovingObjectPosition mp = box.calculateIntercept(start, end);
				if (mp != null) {
					mp.subHit = i;
					double d = start.squareDistanceTo(mp.hitVec);
					if (result == null || d < nearestDistance) {
						result = mp;
						nearestDistance = d;
					}
				}
			}
		}
		if (result != null) {
			//setBlockBounds(list.get(result.subHit));
			int i = result.subHit;
			boxHit = list.get(i).offset(-pos.getX(), -pos.getY(), -pos.getZ());
			result = new MovingObjectPosition(result.hitVec, result.sideHit, pos);
			result.subHit = i;
		}
		return result;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
		if (boxHit != null) {
			ShapeTE te = ShapeTE.get(world, pos);
			if (te != null && te.shape.kind.highlightZones()) {
				setBlockBounds(boxHit);
				return;
			}
		}
		IBlockState state = world.getBlockState(pos);
//		List<AxisAlignedBB> list = getLocalCollisionBoxes(world, pos, state, null);
//		if (list != null)
//			setBlockBounds(Utils.unionOfBoxes(list));
		AxisAlignedBB box = getLocalBounds(world, pos, state, null);
		if (box != null)
			setBlockBounds(box);
		else
			super.setBlockBoundsBasedOnState(world, pos);
	}
	
	public void setBlockBounds(AxisAlignedBB box) {
		setBlockBounds((float)box.minX, (float)box.minY, (float)box.minZ,
			(float)box.maxX, (float)box.maxY, (float)box.maxZ);
	}

	@Override
	public void addCollisionBoxesToList(World world, BlockPos pos, IBlockState state,
		AxisAlignedBB clip, List result, Entity entity)
	{
		List<AxisAlignedBB> list = getGlobalCollisionBoxes(world, pos, state, entity);
		if (list != null)
			for (AxisAlignedBB box : list)
				if (clip.intersectsWith(box))
					result.add(box);
	}
	
	protected List<AxisAlignedBB> getGlobalCollisionBoxes(IBlockAccess world, BlockPos pos,
		IBlockState state, Entity entity)
	{
		ShapeTE te = (ShapeTE)world.getTileEntity(pos);
		if (te != null) {
			Trans3 t = te.localToGlobalTransformation();
			return getCollisionBoxes(te, world, pos, state, t, entity);
		}
		return null;
	}
	
	protected List<AxisAlignedBB> getLocalCollisionBoxes(IBlockAccess world, BlockPos pos,
		IBlockState state, Entity entity)
	{
		ShapeTE te = (ShapeTE)world.getTileEntity(pos);
		if (te != null) {
			Trans3 t = te.localToGlobalTransformation(Vector3.zero);
			return getCollisionBoxes(te, world, pos, state, t, entity);
		}
		return null;
	}
	
	protected AxisAlignedBB getLocalBounds(IBlockAccess world, BlockPos pos,
		IBlockState state, Entity entity)
	{
		ShapeTE te = (ShapeTE)world.getTileEntity(pos);
		if (te != null) {
			Trans3 t = te.localToGlobalTransformation(Vector3.blockCenter);
			return te.shape.kind.getBounds(te, world, pos, state, entity, t);
		}
		return null;
	}

	protected List<AxisAlignedBB> getCollisionBoxes(ShapeTE te,
		IBlockAccess world, BlockPos pos, IBlockState state, Trans3 t, Entity entity)
	{
		List<AxisAlignedBB> list = new ArrayList<AxisAlignedBB>();
		te.shape.kind.addCollisionBoxesToList(te, world, pos, state, entity, t, list);
		return list;
	}

	@Override
	public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
		//System.out.printf("ShapeBlock.canHarvestBlock: by %s\n", player);
		return true;
	}
	
	@Override
	public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, TileEntity te) {
		//System.out.printf("ShapeBlock.harvestBlock: by %s\n", player);
		if (te instanceof ShapeTE) {
			ShapeTE ste = (ShapeTE)te;
			ItemStack stack = BaseUtils.blockStackWithTileEntity(this, 1, ste);
			//System.out.printf("ShapeBlock.harvestBlock: spawning %s\n", stack);
			spawnAsEntity(world, pos, stack);
			if (ste.secondaryBlockState != null) {
				stack = ste.shape.kind.newSecondaryMaterialStack(ste.secondaryBlockState);
				//System.out.printf("ShapeBlock.harvestBlock: spawning %s\n", stack);
				spawnAsEntity(world, pos, stack);
			}
		}
	}

	@Override
	public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos) {
		ShapeTE te = ShapeTE.get(world, pos);
		if (te != null)
			return BaseUtils.blockStackWithTileEntity(this, 1, te);
		else
			return null;
	}

	public IBlockState getBaseBlockState(IBlockAccess world, BlockPos pos) {
		ShapeTE te = getTileEntity(world, pos);
		if (te != null)
			return te.baseBlockState;
		return null;
	}

	@Override
	public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, BlockPos pos) {
		float result = 1.0F;
		IBlockState base = getBaseBlockState(world, pos);
		if (base != null) {
			//System.out.printf("ShapeBlock.getPlayerRelativeBlockHardness: base = %s\n", base);
			result = acBlockStrength(base, player, world, pos);
		}
		return result;
	}
	
	public static float acBlockStrength(IBlockState state, EntityPlayer player, World world, BlockPos pos) {
		float hardness = state.getBlock().getBlockHardness(world, pos);
		if (hardness < 0.0F)
			return 0.0F;
		float strength = player.getBreakSpeed(state, pos) / hardness;
		if (!acCanHarvestBlock(state, player))
			return  strength / 100F;
		else
			return strength / 30F;
	}

	public static boolean acCanHarvestBlock(IBlockState state, EntityPlayer player) {
		Block block = state.getBlock();
		if (block.getMaterial().isToolNotRequired())
			return true;
		ItemStack stack = player.inventory.getCurrentItem();
		//state = state.getBlock().getActualState(state, world, pos);
		String tool = block.getHarvestTool(state);
		if (stack == null || tool == null)
			return player.canHarvestBlock(block);
		int toolLevel = stack.getItem().getHarvestLevel(stack, tool);
		if (toolLevel < 0)
			return player.canHarvestBlock(block);
		else
			return toolLevel >= block.getHarvestLevel(state);
	}
 
	@Override
	public IBlockState getParticleState(IBlockAccess world, BlockPos pos) {
		IBlockState base = getBaseBlockState(world, pos);
		if (base != null)
			return base;
		else
			return getDefaultState();
	}
	
	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
		EnumFacing side, float hitX, float hitY, float hitZ)
	{
		ItemStack stack = player.inventory.getCurrentItem();
		if (stack != null) {
			ShapeTE te = ShapeTE.get(world, pos);
			if (te != null)
				return te.applySecondaryMaterial(stack, player);
		}
		return false;
	}
	
	@Override
	public boolean canRenderInLayer(EnumWorldBlockLayer layer) {
		return true;
	}

	@SideOnly(Side.CLIENT)
	@Override
	public float getAmbientOcclusionLightValue() {
		return 0.8f;
	}

}
