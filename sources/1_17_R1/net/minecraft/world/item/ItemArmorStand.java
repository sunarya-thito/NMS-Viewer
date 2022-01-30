package net.minecraft.world.item;

import java.util.Random;
import net.minecraft.core.BaseBlockPosition;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.Vector3f;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMobSpawn;
import net.minecraft.world.entity.decoration.EntityArmorStand;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.item.context.ItemActionContext;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;

public class ItemArmorStand extends Item {

    public ItemArmorStand(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public EnumInteractionResult a(ItemActionContext itemactioncontext) {
        EnumDirection enumdirection = itemactioncontext.getClickedFace();

        if (enumdirection == EnumDirection.DOWN) {
            return EnumInteractionResult.FAIL;
        } else {
            World world = itemactioncontext.getWorld();
            BlockActionContext blockactioncontext = new BlockActionContext(itemactioncontext);
            BlockPosition blockposition = blockactioncontext.getClickPosition();
            ItemStack itemstack = itemactioncontext.getItemStack();
            Vec3D vec3d = Vec3D.c((BaseBlockPosition) blockposition);
            AxisAlignedBB axisalignedbb = EntityTypes.ARMOR_STAND.m().a(vec3d.getX(), vec3d.getY(), vec3d.getZ());

            if (world.b((Entity) null, axisalignedbb, (entity) -> {
                return true;
            }) && world.getEntities((Entity) null, axisalignedbb).isEmpty()) {
                if (world instanceof WorldServer) {
                    WorldServer worldserver = (WorldServer) world;
                    EntityArmorStand entityarmorstand = (EntityArmorStand) EntityTypes.ARMOR_STAND.createCreature(worldserver, itemstack.getTag(), (IChatBaseComponent) null, itemactioncontext.getEntity(), blockposition, EnumMobSpawn.SPAWN_EGG, true, true);

                    if (entityarmorstand == null) {
                        return EnumInteractionResult.FAIL;
                    }

                    float f = (float) MathHelper.d((MathHelper.g(itemactioncontext.i() - 180.0F) + 22.5F) / 45.0F) * 45.0F;

                    entityarmorstand.setPositionRotation(entityarmorstand.locX(), entityarmorstand.locY(), entityarmorstand.locZ(), f, 0.0F);
                    this.a(entityarmorstand, world.random);
                    // CraftBukkit start
                    if (org.bukkit.craftbukkit.event.CraftEventFactory.callEntityPlaceEvent(itemactioncontext, entityarmorstand).isCancelled()) {
                        return EnumInteractionResult.FAIL;
                    }
                    // CraftBukkit end
                    worldserver.addAllEntities(entityarmorstand);
                    world.playSound((EntityHuman) null, entityarmorstand.locX(), entityarmorstand.locY(), entityarmorstand.locZ(), SoundEffects.ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);
                    world.a((Entity) itemactioncontext.getEntity(), GameEvent.ENTITY_PLACE, (Entity) entityarmorstand);
                }

                itemstack.subtract(1);
                return EnumInteractionResult.a(world.isClientSide);
            } else {
                return EnumInteractionResult.FAIL;
            }
        }
    }

    private void a(EntityArmorStand entityarmorstand, Random random) {
        Vector3f vector3f = entityarmorstand.v();
        float f = random.nextFloat() * 5.0F;
        float f1 = random.nextFloat() * 20.0F - 10.0F;
        Vector3f vector3f1 = new Vector3f(vector3f.getX() + f, vector3f.getY() + f1, vector3f.getZ());

        entityarmorstand.setHeadPose(vector3f1);
        vector3f = entityarmorstand.w();
        f = random.nextFloat() * 10.0F - 5.0F;
        vector3f1 = new Vector3f(vector3f.getX(), vector3f.getY() + f, vector3f.getZ());
        entityarmorstand.setBodyPose(vector3f1);
    }
}
