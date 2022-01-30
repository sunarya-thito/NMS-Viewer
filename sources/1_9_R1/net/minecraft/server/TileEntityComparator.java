package net.minecraft.server;

public class TileEntityComparator extends TileEntity {

    private int a;

    public TileEntityComparator() {}

    public void save(NBTTagCompound nbttagcompound) {
        super.save(nbttagcompound);
        nbttagcompound.setInt("OutputSignal", this.a);
    }

    public void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        this.a = nbttagcompound.getInt("OutputSignal");
    }

    public int b() {
        return this.a;
    }

    public void a(int i) {
        this.a = i;
    }
}
