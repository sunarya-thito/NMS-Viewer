package net.minecraft.server;

import java.util.ArrayList; // Spigot
import java.util.List; // Spigot
import java.util.WeakHashMap;

public class NavigationListener implements IWorldAccess {

    private static final Object a = new Object();
    private final List<NavigationAbstract> navigators = new ArrayList<NavigationAbstract>(); // Spigot

    public NavigationListener() {}

    public void a(NavigationAbstract navigationabstract) {
        //this.b.put(navigationabstract, NavigationListener.a); // Spigot
        new Throwable("Unexpected NavigationListener add").printStackTrace(); // Spigot
    }

    public void a(World world, BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1, int i) {
        if (this.a(world, blockposition, iblockdata, iblockdata1)) {
            /* // Spigot start
            NavigationAbstract[] anavigationabstract = (NavigationAbstract[]) this.b.keySet().toArray(new NavigationAbstract[0]);
            NavigationAbstract[] anavigationabstract1 = anavigationabstract;
            int j = anavigationabstract.length;

            */
            // DO NOT USE AN ITERATOR! This must be a for (;;) to avoid CME.
            // This is perfectly safe, as additions are ok to be done in this iteration
            // and Removals are queued instead of immediate.
            for (int k = 0; k < this.navigators.size(); ++k) {
                NavigationAbstract navigationabstract = this.navigators.get(k);
                // Spigot end

                if (navigationabstract != null && !navigationabstract.i()) {
                    PathEntity pathentity = navigationabstract.k();

                    if (pathentity != null && !pathentity.b() && pathentity.d() != 0) {
                        PathPoint pathpoint = navigationabstract.c.c();
                        double d0 = blockposition.distanceSquared(((double) pathpoint.a + navigationabstract.a.locX) / 2.0D, ((double) pathpoint.b + navigationabstract.a.locY) / 2.0D, ((double) pathpoint.c + navigationabstract.a.locZ) / 2.0D);
                        int l = (pathentity.d() - pathentity.e()) * (pathentity.d() - pathentity.e());

                        if (d0 < (double) l) {
                            navigationabstract.j();
                        }
                    }
                }
            }

        }
    }

    protected boolean a(World world, BlockPosition blockposition, IBlockData iblockdata, IBlockData iblockdata1) {
        AxisAlignedBB axisalignedbb = iblockdata.d(world, blockposition);
        AxisAlignedBB axisalignedbb1 = iblockdata1.d(world, blockposition);

        return axisalignedbb != axisalignedbb1 && (axisalignedbb == null || !axisalignedbb.equals(axisalignedbb1));
    }

    public void a(BlockPosition blockposition) {}

    public void a(int i, int j, int k, int l, int i1, int j1) {}

    public void a(EntityHuman entityhuman, SoundEffect soundeffect, SoundCategory soundcategory, double d0, double d1, double d2, float f, float f1) {}

    public void a(int i, boolean flag, double d0, double d1, double d2, double d3, double d4, double d5, int... aint) {}

    public void a(Entity entity) {
        // Spigot start
        if (entity instanceof EntityInsentient) {
            this.navigators.add(((EntityInsentient) entity).navigation);
        }
        // Spigot end
    }

    public void b(Entity entity) {
        // Spigot start
        if (entity instanceof EntityInsentient) {
            this.navigators.remove(((EntityInsentient) entity).navigation);
        }
        // Spigot end
    }

    public void a(SoundEffect soundeffect, BlockPosition blockposition) {}

    public void a(int i, BlockPosition blockposition, int j) {}

    public void a(EntityHuman entityhuman, int i, BlockPosition blockposition, int j) {}

    public void b(int i, BlockPosition blockposition, int j) {}
}
