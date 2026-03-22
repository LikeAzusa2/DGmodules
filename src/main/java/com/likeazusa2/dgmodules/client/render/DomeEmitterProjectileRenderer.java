package com.likeazusa2.dgmodules.client.render;

import com.likeazusa2.dgmodules.entity.DomeEmitterProjectileEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;

public class DomeEmitterProjectileRenderer extends ThrownItemRenderer<DomeEmitterProjectileEntity> {
    public DomeEmitterProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
