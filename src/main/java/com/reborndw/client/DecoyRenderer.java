package com.reborndw.client;

import com.reborndw.entity.MistDecoyEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidArmorModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/** Draws a Mist decoy as its caster: their skin, armor and held items. */
public class DecoyRenderer extends HumanoidMobRenderer<MistDecoyEntity, PlayerModel<MistDecoyEntity>> {
    public DecoyRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new PlayerModel<>(ctx.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        addLayer(new HumanoidArmorLayer<>(this,
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidArmorModel<>(ctx.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                ctx.getModelManager()));
    }

    @Override
    public ResourceLocation getTextureLocation(MistDecoyEntity entity) {
        return entity.owner().map(uuid -> {
            var conn = Minecraft.getInstance().getConnection();
            PlayerInfo info = conn == null ? null : conn.getPlayerInfo(uuid);
            return info != null ? info.getSkin().texture() : DefaultPlayerSkin.get(uuid).texture();
        }).orElse(DefaultPlayerSkin.getDefaultTexture());
    }

    @Override
    protected boolean shouldShowName(MistDecoyEntity entity) {
        return false;
    }
}
