package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.math.Quaternion;
import com.mojang.math.Vector3fa;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;
import net.minecraft.advancements.CriterionTriggers;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.server.level.EntityPlayer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.monster.ICrossbow;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityArrow;
import net.minecraft.world.entity.projectile.EntityFireworks;
import net.minecraft.world.entity.projectile.IProjectile;
import net.minecraft.world.item.enchantment.EnchantmentManager;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;

public class ItemCrossbow extends ItemProjectileWeapon implements ItemVanishable {

    private static final String TAG_CHARGED = "Charged";
    private static final String TAG_CHARGED_PROJECTILES = "ChargedProjectiles";
    private static final int MAX_CHARGE_DURATION = 25;
    public static final int DEFAULT_RANGE = 8;
    private boolean startSoundPlayed = false;
    private boolean midLoadSoundPlayed = false;
    private static final float START_SOUND_PERCENT = 0.2F;
    private static final float MID_SOUND_PERCENT = 0.5F;
    private static final float ARROW_POWER = 3.15F;
    private static final float FIREWORK_POWER = 1.6F;

    public ItemCrossbow(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public Predicate<ItemStack> e() {
        return ItemCrossbow.ARROW_OR_FIREWORK;
    }

    @Override
    public Predicate<ItemStack> b() {
        return ItemCrossbow.ARROW_ONLY;
    }

    @Override
    public InteractionResultWrapper<ItemStack> a(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        if (d(itemstack)) {
            a(world, entityhuman, enumhand, itemstack, o(itemstack), 1.0F);
            a(itemstack, false);
            return InteractionResultWrapper.consume(itemstack);
        } else if (!entityhuman.h(itemstack).isEmpty()) {
            if (!d(itemstack)) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
                entityhuman.c(enumhand);
            }

            return InteractionResultWrapper.consume(itemstack);
        } else {
            return InteractionResultWrapper.fail(itemstack);
        }
    }

    private static float o(ItemStack itemstack) {
        return a(itemstack, Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }

    @Override
    public void a(ItemStack itemstack, World world, EntityLiving entityliving, int i) {
        int j = this.b(itemstack) - i;
        float f = a(j, itemstack);

        if (f >= 1.0F && !d(itemstack) && a(entityliving, itemstack)) {
            a(itemstack, true);
            SoundCategory soundcategory = entityliving instanceof EntityHuman ? SoundCategory.PLAYERS : SoundCategory.HOSTILE;

            world.playSound((EntityHuman) null, entityliving.locX(), entityliving.locY(), entityliving.locZ(), SoundEffects.CROSSBOW_LOADING_END, soundcategory, 1.0F, 1.0F / (world.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }

    }

    private static boolean a(EntityLiving entityliving, ItemStack itemstack) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.MULTISHOT, itemstack);
        int j = i == 0 ? 1 : 3;
        boolean flag = entityliving instanceof EntityHuman && ((EntityHuman) entityliving).getAbilities().instabuild;
        ItemStack itemstack1 = entityliving.h(itemstack);
        ItemStack itemstack2 = itemstack1.cloneItemStack();

        for (int k = 0; k < j; ++k) {
            if (k > 0) {
                itemstack1 = itemstack2.cloneItemStack();
            }

            if (itemstack1.isEmpty() && flag) {
                itemstack1 = new ItemStack(Items.ARROW);
                itemstack2 = itemstack1.cloneItemStack();
                // CraftBukkit start - SPIGOT-4870, MC-150847
            } else if (itemstack1.isEmpty()) {
                return false;
                // CraftBukkit end
            }

            if (!a(entityliving, itemstack, itemstack1, k > 0, flag)) {
                return false;
            }
        }

        return true;
    }

    private static boolean a(EntityLiving entityliving, ItemStack itemstack, ItemStack itemstack1, boolean flag, boolean flag1) {
        if (itemstack1.isEmpty()) {
            return false;
        } else {
            boolean flag2 = flag1 && itemstack1.getItem() instanceof ItemArrow;
            ItemStack itemstack2;

            if (!flag2 && !flag1 && !flag) {
                itemstack2 = itemstack1.cloneAndSubtract(1);
                if (itemstack1.isEmpty() && entityliving instanceof EntityHuman) {
                    ((EntityHuman) entityliving).getInventory().g(itemstack1);
                }
            } else {
                itemstack2 = itemstack1.cloneItemStack();
            }

            b(itemstack, itemstack2);
            return true;
        }
    }

    public static boolean d(ItemStack itemstack) {
        NBTTagCompound nbttagcompound = itemstack.getTag();

        return nbttagcompound != null && nbttagcompound.getBoolean("Charged");
    }

    public static void a(ItemStack itemstack, boolean flag) {
        NBTTagCompound nbttagcompound = itemstack.getOrCreateTag();

        nbttagcompound.setBoolean("Charged", flag);
    }

    private static void b(ItemStack itemstack, ItemStack itemstack1) {
        NBTTagCompound nbttagcompound = itemstack.getOrCreateTag();
        NBTTagList nbttaglist;

        if (nbttagcompound.hasKeyOfType("ChargedProjectiles", 9)) {
            nbttaglist = nbttagcompound.getList("ChargedProjectiles", 10);
        } else {
            nbttaglist = new NBTTagList();
        }

        NBTTagCompound nbttagcompound1 = new NBTTagCompound();

        itemstack1.save(nbttagcompound1);
        nbttaglist.add(nbttagcompound1);
        nbttagcompound.set("ChargedProjectiles", nbttaglist);
    }

    private static List<ItemStack> p(ItemStack itemstack) {
        List<ItemStack> list = Lists.newArrayList();
        NBTTagCompound nbttagcompound = itemstack.getTag();

        if (nbttagcompound != null && nbttagcompound.hasKeyOfType("ChargedProjectiles", 9)) {
            NBTTagList nbttaglist = nbttagcompound.getList("ChargedProjectiles", 10);

            if (nbttaglist != null) {
                for (int i = 0; i < nbttaglist.size(); ++i) {
                    NBTTagCompound nbttagcompound1 = nbttaglist.getCompound(i);

                    list.add(ItemStack.a(nbttagcompound1));
                }
            }
        }

        return list;
    }

    private static void q(ItemStack itemstack) {
        NBTTagCompound nbttagcompound = itemstack.getTag();

        if (nbttagcompound != null) {
            NBTTagList nbttaglist = nbttagcompound.getList("ChargedProjectiles", 9);

            nbttaglist.clear();
            nbttagcompound.set("ChargedProjectiles", nbttaglist);
        }

    }

    public static boolean a(ItemStack itemstack, Item item) {
        return p(itemstack).stream().anyMatch((itemstack1) -> {
            return itemstack1.a(item);
        });
    }

    private static void a(World world, EntityLiving entityliving, EnumHand enumhand, ItemStack itemstack, ItemStack itemstack1, float f, boolean flag, float f1, float f2, float f3) {
        if (!world.isClientSide) {
            boolean flag1 = itemstack1.a(Items.FIREWORK_ROCKET);
            Object object;

            if (flag1) {
                object = new EntityFireworks(world, itemstack1, entityliving, entityliving.locX(), entityliving.getHeadY() - 0.15000000596046448D, entityliving.locZ(), true);
            } else {
                object = a(world, entityliving, itemstack, itemstack1);
                if (flag || f3 != 0.0F) {
                    ((EntityArrow) object).pickup = EntityArrow.PickupStatus.CREATIVE_ONLY;
                }
            }

            if (entityliving instanceof ICrossbow) {
                ICrossbow icrossbow = (ICrossbow) entityliving;

                icrossbow.a(icrossbow.getGoalTarget(), itemstack, (IProjectile) object, f3);
            } else {
                Vec3D vec3d = entityliving.h(1.0F);
                Quaternion quaternion = new Quaternion(new Vector3fa(vec3d), f3, true);
                Vec3D vec3d1 = entityliving.e(1.0F);
                Vector3fa vector3fa = new Vector3fa(vec3d1);

                vector3fa.a(quaternion);
                ((IProjectile) object).shoot((double) vector3fa.a(), (double) vector3fa.b(), (double) vector3fa.c(), f1, f2);
            }
            // CraftBukkit start
            org.bukkit.event.entity.EntityShootBowEvent event = org.bukkit.craftbukkit.event.CraftEventFactory.callEntityShootBowEvent(entityliving, itemstack, itemstack1, (Entity) object, entityliving.getRaisedHand(), f, true);
            if (event.isCancelled()) {
                event.getProjectile().remove();
                return;
            }
            // CraftBukkit end

            itemstack.damage(flag1 ? 3 : 1, entityliving, (entityliving1) -> {
                entityliving1.broadcastItemBreak(enumhand);
            });
            // CraftBukkit start
            if (event.getProjectile() == ((Entity) object).getBukkitEntity()) {
                if (!world.addEntity((Entity) object)) {
                    if (entityliving instanceof EntityPlayer) {
                        ((EntityPlayer) entityliving).getBukkitEntity().updateInventory();
                    }
                    return;
                }
            }
            // CraftBukkit end
            world.playSound((EntityHuman) null, entityliving.locX(), entityliving.locY(), entityliving.locZ(), SoundEffects.CROSSBOW_SHOOT, SoundCategory.PLAYERS, 1.0F, f);
        }
    }

    private static EntityArrow a(World world, EntityLiving entityliving, ItemStack itemstack, ItemStack itemstack1) {
        ItemArrow itemarrow = (ItemArrow) (itemstack1.getItem() instanceof ItemArrow ? itemstack1.getItem() : Items.ARROW);
        EntityArrow entityarrow = itemarrow.a(world, itemstack1, entityliving);

        if (entityliving instanceof EntityHuman) {
            entityarrow.setCritical(true);
        }

        entityarrow.a(SoundEffects.CROSSBOW_HIT);
        entityarrow.setShotFromCrossbow(true);
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.PIERCING, itemstack);

        if (i > 0) {
            entityarrow.setPierceLevel((byte) i);
        }

        return entityarrow;
    }

    public static void a(World world, EntityLiving entityliving, EnumHand enumhand, ItemStack itemstack, float f, float f1) {
        List<ItemStack> list = p(itemstack);
        float[] afloat = a(entityliving.getRandom());

        for (int i = 0; i < list.size(); ++i) {
            ItemStack itemstack1 = (ItemStack) list.get(i);
            boolean flag = entityliving instanceof EntityHuman && ((EntityHuman) entityliving).getAbilities().instabuild;

            if (!itemstack1.isEmpty()) {
                if (i == 0) {
                    a(world, entityliving, enumhand, itemstack, itemstack1, afloat[i], flag, f, f1, 0.0F);
                } else if (i == 1) {
                    a(world, entityliving, enumhand, itemstack, itemstack1, afloat[i], flag, f, f1, -10.0F);
                } else if (i == 2) {
                    a(world, entityliving, enumhand, itemstack, itemstack1, afloat[i], flag, f, f1, 10.0F);
                }
            }
        }

        a(world, entityliving, itemstack);
    }

    private static float[] a(Random random) {
        boolean flag = random.nextBoolean();

        return new float[]{1.0F, a(flag, random), a(!flag, random)};
    }

    private static float a(boolean flag, Random random) {
        float f = flag ? 0.63F : 0.43F;

        return 1.0F / (random.nextFloat() * 0.5F + 1.8F) + f;
    }

    private static void a(World world, EntityLiving entityliving, ItemStack itemstack) {
        if (entityliving instanceof EntityPlayer) {
            EntityPlayer entityplayer = (EntityPlayer) entityliving;

            if (!world.isClientSide) {
                CriterionTriggers.SHOT_CROSSBOW.a(entityplayer, itemstack);
            }

            entityplayer.b(StatisticList.ITEM_USED.b(itemstack.getItem()));
        }

        q(itemstack);
    }

    @Override
    public void a(World world, EntityLiving entityliving, ItemStack itemstack, int i) {
        if (!world.isClientSide) {
            int j = EnchantmentManager.getEnchantmentLevel(Enchantments.QUICK_CHARGE, itemstack);
            SoundEffect soundeffect = this.a(j);
            SoundEffect soundeffect1 = j == 0 ? SoundEffects.CROSSBOW_LOADING_MIDDLE : null;
            float f = (float) (itemstack.o() - i) / (float) k(itemstack);

            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                world.playSound((EntityHuman) null, entityliving.locX(), entityliving.locY(), entityliving.locZ(), soundeffect, SoundCategory.PLAYERS, 0.5F, 1.0F);
            }

            if (f >= 0.5F && soundeffect1 != null && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                world.playSound((EntityHuman) null, entityliving.locX(), entityliving.locY(), entityliving.locZ(), soundeffect1, SoundCategory.PLAYERS, 0.5F, 1.0F);
            }
        }

    }

    @Override
    public int b(ItemStack itemstack) {
        return k(itemstack) + 3;
    }

    public static int k(ItemStack itemstack) {
        int i = EnchantmentManager.getEnchantmentLevel(Enchantments.QUICK_CHARGE, itemstack);

        return i == 0 ? 25 : 25 - 5 * i;
    }

    @Override
    public EnumAnimation c(ItemStack itemstack) {
        return EnumAnimation.CROSSBOW;
    }

    private SoundEffect a(int i) {
        switch (i) {
            case 1:
                return SoundEffects.CROSSBOW_QUICK_CHARGE_1;
            case 2:
                return SoundEffects.CROSSBOW_QUICK_CHARGE_2;
            case 3:
                return SoundEffects.CROSSBOW_QUICK_CHARGE_3;
            default:
                return SoundEffects.CROSSBOW_LOADING_START;
        }
    }

    private static float a(int i, ItemStack itemstack) {
        float f = (float) i / (float) k(itemstack);

        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void a(ItemStack itemstack, @Nullable World world, List<IChatBaseComponent> list, TooltipFlag tooltipflag) {
        List<ItemStack> list1 = p(itemstack);

        if (d(itemstack) && !list1.isEmpty()) {
            ItemStack itemstack1 = (ItemStack) list1.get(0);

            list.add((new ChatMessage("item.minecraft.crossbow.projectile")).c(" ").addSibling(itemstack1.G()));
            if (tooltipflag.a() && itemstack1.a(Items.FIREWORK_ROCKET)) {
                List<IChatBaseComponent> list2 = Lists.newArrayList();

                Items.FIREWORK_ROCKET.a(itemstack1, world, (List) list2, tooltipflag);
                if (!list2.isEmpty()) {
                    for (int i = 0; i < list2.size(); ++i) {
                        list2.set(i, (new ChatComponentText("  ")).addSibling((IChatBaseComponent) list2.get(i)).a(EnumChatFormat.GRAY));
                    }

                    list.addAll(list2);
                }
            }

        }
    }

    @Override
    public boolean l(ItemStack itemstack) {
        return itemstack.a((Item) this);
    }

    @Override
    public int d() {
        return 8;
    }
}
