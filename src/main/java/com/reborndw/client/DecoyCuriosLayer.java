package com.reborndw.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reborndw.RebornDW;
import com.reborndw.entity.MistDecoyEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.CuriosRendererRegistry;

import java.util.HashSet;
import java.util.Set;

/**
 * Draws the caster's visible Curios (any mod's: Artifacts and so on) on their Mist decoy. It reads the caster's own
 * Curios inventory each frame, honouring cosmetic slots and hidden slots, and renders each item on the decoy's body.
 */
public class DecoyCuriosLayer extends RenderLayer<MistDecoyEntity, PlayerModel<MistDecoyEntity>> {
    private static final Set<String> FAILED = new HashSet<>();
    private final RenderLayerParent<MistDecoyEntity, PlayerModel<MistDecoyEntity>> parent;

    public DecoyCuriosLayer(RenderLayerParent<MistDecoyEntity, PlayerModel<MistDecoyEntity>> parent) {
        super(parent);
        this.parent = parent;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, MistDecoyEntity decoy, float limbSwing,
                       float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        Player owner = decoy.owner().map(u -> decoy.level().getPlayerByUUID(u)).orElse(null);
        if (owner == null) return;
        CuriosApi.getCuriosInventory(owner).ifPresent(inv -> inv.getCurios().forEach((id, handler) -> {
            var stacks = handler.getStacks();
            var cosmetics = handler.getCosmeticStacks();
            var renders = handler.getRenders();
            for (int i = 0; i < stacks.getSlots(); i++) {
                boolean visible = i >= renders.size() || renders.get(i);
                ItemStack stack = i < cosmetics.getSlots() ? cosmetics.getStackInSlot(i) : ItemStack.EMPTY;
                boolean cosmetic = !stack.isEmpty();
                if (!cosmetic) {
                    if (!visible) continue;
                    stack = stacks.getStackInSlot(i);
                }
                if (stack.isEmpty()) continue;
                final ItemStack shown = stack;
                SlotContext ctx = new SlotContext(id, decoy, i, cosmetic, visible);
                CuriosRendererRegistry.getRenderer(shown.getItem()).ifPresent(renderer -> {
                    String key = shown.getItem().toString();
                    if (FAILED.contains(key)) return;
                    pose.pushPose();
                    try {
                        renderer.render(shown, ctx, pose, getParentModelHolder(), buffers, light, limbSwing, limbSwingAmount,
                                partialTick, ageInTicks, netHeadYaw, headPitch);
                    } catch (RuntimeException e) {
                        // a renderer that only expects players: skip it from now on rather than crash the game
                        FAILED.add(key);
                        RebornDW.LOGGER.warn("Can't draw curio {} on a Mist decoy: {}", key, e.toString());
                    } finally {
                        pose.popPose();
                    }
                });
            }
        }));
    }

    private RenderLayerParent<MistDecoyEntity, PlayerModel<MistDecoyEntity>> getParentModelHolder() {
        return parent;
    }
}
