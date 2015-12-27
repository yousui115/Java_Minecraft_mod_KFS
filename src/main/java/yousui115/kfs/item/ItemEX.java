package yousui115.kfs.item;

import java.util.List;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import yousui115.kfs.entity.EntityEXMagic;
import yousui115.kfs.entity.EntityMagicBase;
import yousui115.kfs.entity.EntityMagicBase.EnumMagicType;

public class ItemEX extends ItemKFS
{
    //■excelletor の情報
    public static EnumEXInfo[] infoEX = EnumEXInfo.values();

    /**
     * ■こんすとらくた
     * @param material
     */
    public ItemEX(ToolMaterial material)
    {
        super(material);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public int getColorFromItemStack(ItemStack stack, int renderPass)
    {
        switch(renderPass)
        {
            case 0:
                //▼アルファ値
                GlStateManager.enableAlpha();

                // ▼ブレンド
//                GlStateManager.enableBlend();
//                GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

                break;

            default:
                break;
        }

        return 16777215;//0xFFFFFF
    }

    /**
     * Returns the unlocalized name of this item. This version accepts an ItemStack so different stacks can have
     * different names based on their damage or NBT.
     */
    @Override
    public String getUnlocalizedName(ItemStack stack)
    {
        EnumEXInfo info = this.getEXInfoFromExp(stack);
        return super.getUnlocalizedName(stack) + info.level;
    }

    /**
     * ■returns a list of items with the same ID, but different meta (eg: dye returns 16 items)
     *
     * @param subItems The List of sub-items. This is a List of ItemStacks.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void getSubItems(Item itemIn, CreativeTabs creativeTabsIn, List itemListIn)
    {
        //■Level1
        ItemStack stack1 = new ItemStack(this, 1);
        itemListIn.add(stack1);//クリエイティブタブのアイテムリストに追加

        //■Level2,3
        for (int idx = 0; idx < this.infoEX.length - 1; idx++)
        {
            ItemStack stack2 = new ItemStack(this, 1);
            this.setExp(stack2, infoEX[idx].nextExp);
            itemListIn.add(stack2);//クリエイティブタブのアイテムリストに追加
        }
    }

    /* ======================================== FORGE START =====================================*/

    /**
     * Called when the player Left Clicks (attacks) an entity.
     * Processed before damage is done, if return value is true further processing is canceled
     * and the entity is not attacked.
     *
     * @param stack The Item being used
     * @param player The player that is attacking
     * @param entity The entity being attacked
     * @return True to cancel the rest of the interaction.
     */
    @Override
    public boolean onLeftClickEntity(ItemStack stack, EntityPlayer player, Entity entity)
    {
        //■経験値取得前の状態取得
        EnumEXInfo info = this.getEXInfoFromExp(stack);

        //■経験値取得処理(必ず取得するとは言ってない)
        if (entity instanceof EntityLivingBase) { this.addExp(stack, 1); }
        if (entity instanceof IMob) { this.addExp(stack, 1); }

        //■状態が変わってる = レベルが上がった
        if (player instanceof EntityPlayerSP && info != this.getEXInfoFromExp(stack))
        {
            //■お知らせ！
            player.addChatMessage(new ChatComponentText("Excellector level up!"));
        }
        return false;
    }

    /* ======================================== FORGE END =====================================*/

    /* ======================================== イカ、自作 =====================================*/

    /**
     * ■魔法剣生成
     * @param stack
     * @param worldIn
     * @param playerIn
     * @return
     */
    @Override
    protected EntityMagicBase[] createMagic(ItemStack stackIn, World worldIn, EntityPlayer playerIn)
    {
        if (!this.getEXInfoFromExp(stackIn).canMagic) { return null; }

        EntityMagicBase[] base = {  new EntityEXMagic(worldIn, playerIn, EnumMagicType.EX_L),
                                    new EntityEXMagic(worldIn, playerIn, EnumMagicType.EX_M),
                                    new EntityEXMagic(worldIn, playerIn, EnumMagicType.EX_S)};
        return base;
    }

    //他Modの干渉があるとは思わないけど一応ドメイン形式で
    protected static final String TAG_EXP_STR = "kfs.ex.exp";

    /**
     * ■経験値の設定等
     * @param stackIn
     * @return
     */
    public int getExp(ItemStack stackIn)    //TODO 引数はNBTTagCompoundにすべきか
    {
        NBTTagCompound nbt = this.getNBTTag(stackIn);
        if (!nbt.hasKey(TAG_EXP_STR))
        {
            this.setExp(stackIn, 0);
        }
        return nbt.getInteger(TAG_EXP_STR);
    }

    protected void setExp(ItemStack stackIn, int expIn)
    {
        expIn = MathHelper.clamp_int(expIn, 0, EnumEXInfo.level3.nextExp);
        NBTTagCompound nbt = this.getNBTTag(stackIn);
        nbt.setInteger(TAG_EXP_STR, expIn);
    }

    protected void addExp(ItemStack stackIn, int expIn)
    {
        int limit = EnumEXInfo.level3.nextExp - this.getExp(stackIn);   //上限あふれ防止
        int exp = this.getExp(stackIn) + (limit > expIn ? expIn : limit);

        this.setExp(stackIn, exp);
    }

    /**
     * ■経験値に応じたexcellectorの情報
     * @param expIn
     * @return
     */
    public EnumEXInfo getEXInfoFromExp(ItemStack stackIn)
    {
        int expIn = this.getExp(stackIn);
        int idx = 0;
        EnumEXInfo info;
        do
        {
            info = infoEX[idx];
            if (!info.canGrowth || expIn < info.nextExp) { break; }
        }
        while (++idx < infoEX.length);

        return info;
    }

    /**
     * ■Excellectorの状態情報
     *
     */
    public enum EnumEXInfo
    {
//        level1( 1,              1000, false),
//        level2( 2,              3000, false),
//        level3( 3, Integer.MAX_VALUE,  true);
        level1( 1,                  5,  true, false),
        level2( 2, level1.nextExp + 5,  true, false),
        level3( 3,     level2.nextExp, false,  true);

        public final int level;
        public final int nextExp;
        public final boolean canGrowth; //growth = 成長
        public final boolean canMagic;

        EnumEXInfo(int levelIn, int nextIn, boolean canGrowthIn, boolean canMagicIn)
        {
            level     = levelIn;
            nextExp   = nextIn;
            canGrowth = canGrowthIn;
            canMagic  = canMagicIn;
        }
    }

}
