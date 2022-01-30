package net.minecraft.world.entity.boss.enderdragon;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.nbt.GameProfileSerializer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockFireAbstract;
import net.minecraft.world.level.dimension.end.EnderDragonBattle;

// CraftBukkit start
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.ExplosionPrimeEvent;
// CraftBukkit end

public class EntityEnderCrystal extends Entity {

    private static final DataWatcherObject<Optional<BlockPosition>> DATA_BEAM_TARGET = DataWatcher.a(EntityEnderCrystal.class, DataWatcherRegistry.OPTIONAL_BLOCK_POS);
    private static final DataWatcherObject<Boolean> DATA_SHOW_BOTTOM = DataWatcher.a(EntityEnderCrystal.class, DataWatcherRegistry.BOOLEAN);
    public int time;

    public EntityEnderCrystal(EntityTypes<? extends EntityEnderCrystal> entitytypes, World world) {
        super(entitytypes, world);
        this.blocksBuilding = true;
        this.time = this.random.nextInt(100000);
    }

    public EntityEnderCrystal(World world, double d0, double d1, double d2) {
        this(EntityTypes.END_CRYSTAL, world);
        this.setPosition(d0, d1, d2);
    }

    @Override
    protected Entity.MovementEmission aI() {
        return Entity.MovementEmission.NONE;
    }

    @Override
    protected void initDatawatcher() {
        this.getDataWatcher().register(EntityEnderCrystal.DATA_BEAM_TARGET, Optional.empty());
        this.getDataWatcher().register(EntityEnderCrystal.DATA_SHOW_BOTTOM, true);
    }

    @Override
    public void tick() {
        ++this.time;
        if (this.level instanceof WorldServer) {
            BlockPosition blockposition = this.getChunkCoordinates();

            if (((WorldServer) this.level).getDragonBattle() != null && this.level.getType(blockposition).isAir()) {
                // CraftBukkit start
                if (!CraftEventFactory.callBlockIgniteEvent(this.level, blockposition, this).isCancelled()) {
                    this.level.setTypeUpdate(blockposition, BlockFireAbstract.a((IBlockAccess) this.level, blockposition));
                }
                // CraftBukkit end
            }
        }

    }

    @Override
    protected void saveData(NBTTagCompound nbttagcompound) {
        if (this.getBeamTarget() != null) {
            nbttagcompound.set("BeamTarget", GameProfileSerializer.a(this.getBeamTarget()));
        }

        nbttagcompound.setBoolean("ShowBottom", this.isShowingBottom());
    }

    @Override
    protected void loadData(NBTTagCompound nbttagcompound) {
        if (nbttagcompound.hasKeyOfType("BeamTarget", 10)) {
            this.setBeamTarget(GameProfileSerializer.b(nbttagcompound.getCompound("BeamTarget")));
        }

        if (nbttagcompound.hasKeyOfType("ShowBottom", 1)) {
            this.setShowingBottom(nbttagcompound.getBoolean("ShowBottom"));
        }

    }

    @Override
    public boolean isInteractable() {
        return true;
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        if (this.isInvulnerable(damagesource)) {
            return false;
        } else if (damagesource.getEntity() instanceof EntityEnderDragon) {
            return false;
        } else {
            if (!this.isRemoved() && !this.level.isClientSide) {
                // CraftBukkit start - All non-living entities need this
                if (CraftEventFactory.handleNonLivingEntityDamageEvent(this, damagesource, f, false)) {
                    return false;
                }
                // CraftBukkit end
                this.a(Entity.RemovalReason.KILLED);
                if (!damagesource.isExplosion()) {
                    // CraftBukkit start
                    ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), 6.0F, false);
                    this.level.getCraftServer().getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        this.unsetRemoved();
                        return false;
                    }
                    this.level.createExplosion(this, this.locX(), this.locY(), this.locZ(), event.getRadius(), event.getFire(), Explosion.Effect.DESTROY);
                    // CraftBukkit end
                }

                this.a(damagesource);
            }

            return true;
        }
    }

    @Override
    public void killEntity() {
        this.a(DamageSource.GENERIC);
        super.killEntity();
    }

    private void a(DamageSource damagesource) {
        if (this.level instanceof WorldServer) {
            EnderDragonBattle enderdragonbattle = ((WorldServer) this.level).getDragonBattle();

            if (enderdragonbattle != null) {
                enderdragonbattle.a(this, damagesource);
            }
        }

    }

    public void setBeamTarget(@Nullable BlockPosition blockposition) {
        this.getDataWatcher().set(EntityEnderCrystal.DATA_BEAM_TARGET, Optional.ofNullable(blockposition));
    }

    @Nullable
    public BlockPosition getBeamTarget() {
        return (BlockPosition) ((Optional) this.getDataWatcher().get(EntityEnderCrystal.DATA_BEAM_TARGET)).orElse((Object) null);
    }

    public void setShowingBottom(boolean flag) {
        this.getDataWatcher().set(EntityEnderCrystal.DATA_SHOW_BOTTOM, flag);
    }

    public boolean isShowingBottom() {
        return (Boolean) this.getDataWatcher().get(EntityEnderCrystal.DATA_SHOW_BOTTOM);
    }

    @Override
    public boolean a(double d0) {
        return super.a(d0) || this.getBeamTarget() != null;
    }

    @Override
    public ItemStack df() {
        return new ItemStack(Items.END_CRYSTAL);
    }

    @Override
    public Packet<?> getPacket() {
        return new PacketPlayOutSpawnEntity(this);
    }
}
