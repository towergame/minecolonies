package com.minecolonies.coremod.colony.buildings;

import com.minecolonies.api.util.InventoryUtils;
import com.minecolonies.api.util.ItemStackUtils;
import com.minecolonies.blockout.Log;
import com.minecolonies.blockout.views.Window;
import com.minecolonies.coremod.blocks.BlockHutDeliveryman;
import com.minecolonies.coremod.blocks.ModBlocks;
import com.minecolonies.coremod.client.gui.WindowWareHouseBuilding;
import com.minecolonies.coremod.colony.Colony;
import com.minecolonies.coremod.colony.ColonyView;
import com.minecolonies.coremod.tileentities.TileEntityColonyBuilding;
import com.minecolonies.coremod.tileentities.TileEntityRack;
import com.minecolonies.coremod.tileentities.TileEntityWareHouse;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class of the warehouse building.
 */
public class BuildingWareHouse extends AbstractBuilding
{
    /**
     * String describing the Warehouse.
     */
    private static final String WAREHOUSE = "WareHouse";

    /**
     * Tag to store the deliverymen.
     */
    private static final String TAG_DELIVERYMAN = "Deliveryman";

    /**
     * The list of deliverymen registered to this building.
     */
    private static final List<Vec3d> registeredDeliverymen = new ArrayList<>();

    /**
     * The tileEntity of the building.
     */
    private TileEntityWareHouse tileEntity;

    /**
     * Instantiates a new warehouse building.
     *
     * @param c the colony.
     * @param l the location
     */
    public BuildingWareHouse(final Colony c, final BlockPos l)
    {
        super(c, l);
    }

    /**
     * Register deliveryman with the warehouse.
     *
     * @param buildingWorker the building of the worker.
     * @return true if able to register or already registered
     */
    public boolean registerWithWareHouse(BuildingDeliveryman buildingWorker)
    {
        if (registeredDeliverymen.contains(new Vec3d(buildingWorker.getID())))
        {
            return true;
        }

        if (registeredDeliverymen.size() >= getBuildingLevel())
        {
            if (!registeredDeliverymen.isEmpty())
            {
                Log.getLogger().info(getColony().getName() + " " + Arrays.toString(registeredDeliverymen.toArray()));
                checkForRegisteredDeliverymen();
            }
            return false;
        }

        registeredDeliverymen.add(new Vec3d(buildingWorker.getID()));
        return true;
    }

    /**
     * Check the registered deliverymen and see if one of their huts got destroyed.
     */
    private void checkForRegisteredDeliverymen()
    {
        final List<Vec3d> registeredDeliverymenCopy = new ArrayList<>(registeredDeliverymen);
        for (final Vec3d pos : registeredDeliverymenCopy)
        {
            final Colony colony = getColony();
            if (colony != null && colony.getWorld() != null
                    && (!(colony.getWorld().getBlockState(new BlockPos(pos)) instanceof BlockHutDeliveryman) || colony.isCoordInColony(colony.getWorld(), new BlockPos(pos))))
            {
                registeredDeliverymen.remove(pos);
            }
        }
    }

    @Override
    public void registerBlockPosition(@NotNull final Block block, @NotNull final BlockPos pos, @NotNull final World world)
    {
        if (block instanceof BlockChest && world != null)
        {
            final TileEntity entity = getColony().getWorld().getTileEntity(pos);
            if (entity instanceof TileEntityChest)
            {
                handleBuildingOverChest(pos, (TileEntityChest) entity, world);
            }
        }
        super.registerBlockPosition(block, pos,world);
    }

    /**
     * Handles the chest placement.
     *
     * @param pos   at pos.
     * @param chest the entity.
     * @param world the world.
     */
    public static void handleBuildingOverChest(@NotNull final BlockPos pos, final TileEntityChest chest, final World world)
    {
        final List<ItemStack> inventory = new ArrayList<>();
        final int size = chest.getSingleChestHandler().getSlots();
        for (int slot = 0; slot < size; slot++)
        {
            final ItemStack stack = chest.getSingleChestHandler().getStackInSlot(slot);
            if (!ItemStackUtils.isEmpty(stack))
            {
                inventory.add(stack.copy());
            }
            chest.getSingleChestHandler().extractItem(slot, Integer.MAX_VALUE, false);
        }

        world.setBlockState(pos, ModBlocks.blockRack.getDefaultState(), 0x03);
        final TileEntity entity = world.getTileEntity(pos);
        if (entity instanceof TileEntityRack)
        {
            for (final ItemStack stack : inventory)
            {
                if (!ItemStackUtils.isEmpty(stack))
                {
                    InventoryUtils.addItemStackToItemHandler(((TileEntityRack) entity).getInventory(), stack);
                }
            }
        }
    }

    /**
     * Check if deliveryman is allowed to access warehouse.
     *
     * @param buildingWorker the building of the deliveryman.
     * @return true if able to.
     */
    public boolean canAccessWareHouse(BuildingDeliveryman buildingWorker)
    {
        return registeredDeliverymen.contains(new Vec3d(buildingWorker.getID()));
    }

    /**
     * Get the deliverymen connected with this building.
     *
     * @return the unmodifiable List of positions of them.
     */
    public List<Vec3d> getRegisteredDeliverymen()
    {
        return Collections.unmodifiableList(registeredDeliverymen);
    }

    @NotNull
    @Override
    public String getSchematicName()
    {
        return WAREHOUSE;
    }

    @Override
    public int getMaxBuildingLevel()
    {
        return 5;
    }

    @Override
    public void readFromNBT(@NotNull final NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        registeredDeliverymen.clear();
        final NBTTagList deliverymanTagList = compound.getTagList(TAG_DELIVERYMAN, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < deliverymanTagList.tagCount(); i++)
        {
            final BlockPos pos = NBTUtil.getPosFromTag(deliverymanTagList.getCompoundTagAt(i));
            if (getColony() != null && getColony().getBuilding(pos) instanceof AbstractBuildingWorker && !registeredDeliverymen.contains(new Vec3d(pos)))
            {
                registeredDeliverymen.add(new Vec3d(pos));
            }
        }
    }

    @Override
    public void writeToNBT(@NotNull final NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        @NotNull final NBTTagList levelTagList = new NBTTagList();
        for (@NotNull final Vec3d deliverymanBuilding : registeredDeliverymen)
        {
            levelTagList.appendTag(NBTUtil.createPosTag(new BlockPos(deliverymanBuilding)));
        }
        compound.setTag(TAG_DELIVERYMAN, levelTagList);
    }

    /**
     * Returns the tile entity that belongs to the colony building.
     *
     * @return {@link TileEntityColonyBuilding} object of the building.
     */
    @Override
    public TileEntityWareHouse getTileEntity()
    {
        final Colony colony = getColony();
        if ((tileEntity == null || tileEntity.isInvalid()) && colony != null && colony.getWorld().getBlockState(this.getLocation()).getBlock() != null)
        {
            final TileEntity te = getColony().getWorld().getTileEntity(this.getLocation());
            if (te instanceof TileEntityWareHouse)
            {
                tileEntity = (TileEntityWareHouse) te;
                if (tileEntity.getBuilding() == null)
                {
                    tileEntity.setColony(colony);
                    tileEntity.setBuilding(this);
                }
            }
        }

        return tileEntity;
    }

    @Override
    public void serializeToView(@NotNull final ByteBuf buf)
    {
        super.serializeToView(buf);
    }

    /**
     * BuildingDeliveryman View.
     */
    public static class View extends AbstractBuildingHut.View
    {

        /**
         * Instantiate the deliveryman view.
         *
         * @param c the colonyview to put it in
         * @param l the positon
         */
        public View(final ColonyView c, final BlockPos l)
        {
            super(c, l);
        }

        //todo add specialized view for the warehouse later.
        @NotNull
        @Override
        public Window getWindow()
        {
            return new WindowWareHouseBuilding(this);
        }

        @Override
        public void deserialize(@NotNull final ByteBuf buf)
        {
            super.deserialize(buf);
        }
    }
}
