package mekanism.common.lib.inventory;

import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import mekanism.common.Mekanism;
import mekanism.common.util.InventoryUtils;
import mekanism.common.util.StackUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.items.IItemHandler;

public abstract class TransitRequest {

    private final TransitResponse EMPTY = new TransitResponse(ItemStack.EMPTY, null);

    public static SimpleTransitRequest simple(ItemStack stack) {
        return new SimpleTransitRequest(stack);
    }

    public static TransitRequest anyItem(TileEntity tile, Direction side, int amount) {
        return definedItem(tile, side, amount, Finder.ANY);
    }

    public static TransitRequest definedItem(TileEntity tile, Direction side, int amount, Finder finder) {
        return definedItem(tile, side, 1, amount, finder);
    }

    public static TransitRequest definedItem(TileEntity tile, Direction side, int min, int max, Finder finder) {
        TileTransitRequest ret = new TileTransitRequest(tile, side);
        IItemHandler inventory = InventoryUtils.assertItemHandler("TransitRequest", tile, side.getOpposite());
        if (inventory == null) {
            return ret;
        }
        // count backwards- we start from the bottom of the inventory and go back for consistency
        for (int i = inventory.getSlots() - 1; i >= 0; i--) {
            ItemStack stack = inventory.extractItem(i, max, true);

            if (!stack.isEmpty() && finder.modifies(stack)) {
                HashedItem hashed = new HashedItem(stack);
                int toUse = Math.min(stack.getCount(), max - ret.getCount(hashed));
                if (toUse == 0) {
                    continue; // continue if we don't need anymore of this item type
                }
                ret.addItem(StackUtils.size(stack, toUse), i);
            }
        }
        // remove items that we don't have enough of
        ret.getItemMap().entrySet().removeIf(entry -> entry.getValue().getTotalCount() < min);
        return ret;
    }

    public abstract Collection<? extends ItemData> getItemData();

    public boolean isEmpty() {
        return getItemData().isEmpty();
    }

    public TransitResponse createResponse(ItemStack inserted, ItemData data) {
        return new TransitResponse(inserted, data);
    }

    public TransitResponse createSimpleResponse() {
        ItemData data = getItemData().stream().findFirst().orElse(null);
        return data != null ? createResponse(data.itemType.createStack(data.totalCount), data) : null;
    }

    public TransitResponse getEmptyResponse() {
        return EMPTY;
    }

    public static class TransitResponse {

        private final ItemStack inserted;
        private final ItemData slotData;

        public TransitResponse(ItemStack inserted, ItemData slotData) {
            this.inserted = inserted;
            this.slotData = slotData;
        }

        public int getSendingAmount() {
            return inserted.getCount();
        }

        public ItemData getSlotData() {
            return slotData;
        }

        public ItemStack getStack() {
            return inserted;
        }

        public boolean isEmpty() {
            return inserted.isEmpty() || slotData.getTotalCount() == 0;
        }

        public ItemStack getRejected() {
            return StackUtils.size(slotData.getStack(), slotData.getStack().getCount() - getSendingAmount());
        }

        public ItemStack use(int amount) {
            return slotData.use(amount);
        }

        public ItemStack useAll() {
            return use(getSendingAmount());
        }
    }

    public static class ItemData {

        private final HashedItem itemType;
        protected int totalCount;

        public ItemData(HashedItem itemType) {
            this.itemType = itemType;
        }

        public HashedItem getItemType() {
            return itemType;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public ItemStack getStack() {
            return getItemType().createStack(getTotalCount());
        }

        public ItemStack use(int amount) {
            Mekanism.logger.error("Can't 'use' with this type of TransitResponse: " + this);
            return ItemStack.EMPTY;
        }
    }

    public static class SimpleTransitRequest extends TransitRequest {

        private final List<ItemData> slotData = new ArrayList<>();

        public SimpleTransitRequest(ItemStack stack) {
            slotData.add(new SimpleItemData(stack));
        }

        @Override
        public Collection<ItemData> getItemData() {
            return slotData;
        }

        public static class SimpleItemData extends ItemData {

            public SimpleItemData(ItemStack stack) {
                super(new HashedItem(stack));
                totalCount = stack.getCount();
            }
        }
    }

    public static class TileTransitRequest extends TransitRequest {

        private final TileEntity tile;
        private final Direction side;
        private final Map<HashedItem, TileItemData> itemMap = new LinkedHashMap<>();

        public TileTransitRequest(TileEntity tile, Direction side) {
            this.tile = tile;
            this.side = side;
        }

        public void addItem(ItemStack stack, int slot) {
            HashedItem hashed = new HashedItem(stack);
            itemMap.computeIfAbsent(hashed, TileItemData::new).addSlot(slot, stack);
        }

        public int getCount(HashedItem itemType) {
            ItemData data = itemMap.get(itemType);
            return data != null ? data.getTotalCount() : 0;
        }

        public Map<HashedItem, TileItemData> getItemMap() {
            return itemMap;
        }

        @Override
        public Collection<TileItemData> getItemData() {
            return itemMap.values();
        }

        public class TileItemData extends ItemData {

            private final Int2IntMap slotMap = new Int2IntOpenHashMap();

            public TileItemData(HashedItem itemType) {
                super(itemType);
            }

            public void addSlot(int id, ItemStack stack) {
                slotMap.put(id, stack.getCount());
                totalCount += stack.getCount();
            }

            @Override
            public ItemStack use(int amount) {
                IItemHandler handler = InventoryUtils.assertItemHandler("InvStack", tile, side);
                if (handler != null) {
                    for (Int2IntMap.Entry entry : slotMap.int2IntEntrySet()) {
                        int toUse = Math.min(amount, entry.getIntValue());
                        ItemStack ret = handler.extractItem(entry.getIntKey(), toUse, false);
                        boolean stackable = InventoryUtils.areItemsStackable(getItemType().getStack(), ret);
                        if (!stackable || ret.getCount() != toUse) { // be loud if an InvStack's prediction doesn't line up
                            Mekanism.logger.warn("An inventory's returned content " + (!stackable ? "type" : "count") + " does not line up with InvStack's prediction.");
                            Mekanism.logger.warn("InvStack item: " + getItemType().getStack() + ", ret: " + ret);
                            Mekanism.logger.warn("Tile: " + tile + " " + tile.getPos());
                        }
                        amount -= toUse;
                        totalCount -= amount;
                        entry.setValue(totalCount - toUse);
                        if (totalCount == 0) {
                            itemMap.remove(getItemType());
                        }
                        if (amount == 0) {
                            break;
                        }
                    }
                }
                return getStack();
            }
        }
    }
}