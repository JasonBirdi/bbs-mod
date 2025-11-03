package mchorse.bbs_mod.graphics;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class Gizmo3D
{
    private static final float ARROW_LENGTH = 0.12F;
    private static final float ARROW_WIDTH = 0.03F;
    private static final float RING_RADIUS_BLUE = 0.32F;
    private static final float RING_RADIUS_RED = 0.36F;
    private static final float RING_RADIUS_GREEN = 0.4F;
    private static final float RING_THICKNESS = 0.005F;
    private static final float ARROW_START_OFFSET = 0.42F;
    private static final float ORIGIN_SIZE = 0.024F;
    private static final float ORIGIN_OUTLINE = 0.003F;

    private static final int RING_SEGMENTS = 64;

    public enum Axis
    {
        X, Y, Z
    }

    public static void render(MatrixStack stack, float baseScale, float alpha)
    {
        float scale = baseScale * BBSSettings.axesScale.get();

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        renderAxis(builder, stack, Axis.X, scale, alpha);
        renderAxis(builder, stack, Axis.Y, scale, alpha);
        renderAxis(builder, stack, Axis.Z, scale, alpha);

        BufferRenderer.drawWithGlobalProgram(builder.end());

        renderRotationRing(stack, Axis.X, scale, alpha);
        renderRotationRing(stack, Axis.Y, scale, alpha);
        renderRotationRing(stack, Axis.Z, scale, alpha);

        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderAxis(BufferBuilder builder, MatrixStack stack, Axis axis, float scale, float alpha)
    {
        float[] color = getAxisColor(axis);
        float r = color[0];
        float g = color[1];
        float b = color[2];

        stack.push();

        switch (axis)
        {
            case X -> {} // No rotation needed for X axis
            case Y -> stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(90F));
            case Z -> stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90F));
        }

        float arrowStart = ARROW_START_OFFSET * scale;
        float arrowLen = ARROW_LENGTH * scale;
        float arrowWidth = ARROW_WIDTH * scale;

        renderArrowhead(builder, stack, arrowStart, arrowStart + arrowLen, arrowWidth, r, g, b, alpha);

        stack.pop();
    }


    private static void renderArrowhead(BufferBuilder builder, MatrixStack stack, float base, float tip, float width, float r, float g, float b, float a)
    {
        Matrix4f m = stack.peek().getPositionMatrix();
        float half = width / 2F;

        builder.vertex(m, base, -half, -half).color(r, g, b, a).next();
        builder.vertex(m, base, half, -half).color(r, g, b, a).next();
        builder.vertex(m, tip, 0, 0).color(r, g, b, a).next();

        builder.vertex(m, base, half, half).color(r, g, b, a).next();
        builder.vertex(m, base, -half, half).color(r, g, b, a).next();
        builder.vertex(m, tip, 0, 0).color(r, g, b, a).next();

        builder.vertex(m, base, half, -half).color(r, g, b, a).next();
        builder.vertex(m, base, half, half).color(r, g, b, a).next();
        builder.vertex(m, tip, 0, 0).color(r, g, b, a).next();

        builder.vertex(m, base, -half, half).color(r, g, b, a).next();
        builder.vertex(m, base, -half, -half).color(r, g, b, a).next();
        builder.vertex(m, tip, 0, 0).color(r, g, b, a).next();
    }

    private static void renderRotationRing(MatrixStack stack, Axis axis, float scale, float alpha)
    {
        float[] color = getAxisColor(axis);
        float radius = switch (axis)
        {
            case X -> RING_RADIUS_RED * scale;
            case Y -> RING_RADIUS_GREEN * scale;
            case Z -> RING_RADIUS_BLUE * scale;
        };
        float thickness = RING_THICKNESS * scale;

        stack.push();

        switch (axis)
        {
            case X -> stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90F));
            case Y -> stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));
            case Z -> {} // No rotation needed for Z axis (XY plane)
        }

        renderRing(stack, radius, thickness, RING_SEGMENTS, color[0], color[1], color[2], alpha * 0.8F);

        stack.pop();
    }

    private static void renderRing(MatrixStack stack, float radius, float thickness, int segments, float r, float g, float b, float a)
    {
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        float halfThickness = thickness / 2F;
        float inner = Math.max(radius - halfThickness, 0.0001F);
        float outer = radius + halfThickness;

        for (int i = 0; i < segments; i++)
        {
            float t0 = (float) (2 * Math.PI * i / segments);
            float t1 = (float) (2 * Math.PI * (i + 1) / segments);

            float ci0 = (float) Math.cos(t0) * inner;
            float si0 = (float) Math.sin(t0) * inner;
            float co0 = (float) Math.cos(t0) * outer;
            float so0 = (float) Math.sin(t0) * outer;

            float ci1 = (float) Math.cos(t1) * inner;
            float si1 = (float) Math.sin(t1) * inner;
            float co1 = (float) Math.cos(t1) * outer;
            float so1 = (float) Math.sin(t1) * outer;

            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci0, si0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();

            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, co1, so1, 0).color(r, g, b, a).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void renderOriginSquare(MatrixStack stack, float scale, float alpha)
    {
        float size = ORIGIN_SIZE * scale;
        float outline = ORIGIN_OUTLINE * scale;
        float half = size / 2F;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        stack.push();
        stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90F));

        Matrix4f m = stack.peek().getPositionMatrix();

        float inner = half - outline;
        float outer = half + outline;

        builder.vertex(m, inner, inner, 0).color(1F, 1F, 1F, alpha).next();
        builder.vertex(m, inner, -inner, 0).color(1F, 1F, 1F, alpha).next();
        builder.vertex(m, -inner, -inner, 0).color(1F, 1F, 1F, alpha).next();

        builder.vertex(m, inner, inner, 0).color(1F, 1F, 1F, alpha).next();
        builder.vertex(m, -inner, -inner, 0).color(1F, 1F, 1F, alpha).next();
        builder.vertex(m, -inner, inner, 0).color(1F, 1F, 1F, alpha).next();

        float purpleR = 0.5F;
        float purpleG = 0.0F;
        float purpleB = 0.5F;

        builder.vertex(m, outer, outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, outer, inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, inner, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, outer, outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, outer, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, outer, -inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, outer, -outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, -outer, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, outer, -inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, -outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, -inner, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, -inner, outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, inner, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, -inner, outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, -inner, inner, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, -inner, -inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, -inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, -outer, 0).color(purpleR, purpleG, purpleB, alpha).next();

        builder.vertex(m, -inner, -inner, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, inner, -outer, 0).color(purpleR, purpleG, purpleB, alpha).next();
        builder.vertex(m, -inner, -outer, 0).color(purpleR, purpleG, purpleB, alpha).next();

        stack.pop();

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static float[] getAxisColor(Axis axis)
    {
        return switch (axis)
        {
            case X -> new float[]{1F, 0F, 0F};
            case Y -> new float[]{0F, 1F, 0F};
            case Z -> new float[]{0F, 0F, 1F};
        };
    }

    public static float getAxisLength(float scale)
    {
        return ARROW_LENGTH * scale * BBSSettings.axesScale.get();
    }

    public static float getRingRadius(float scale)
    {
        return RING_RADIUS_GREEN * scale * BBSSettings.axesScale.get();
    }

    public static float getRingRadiusBlue(float scale)
    {
        return RING_RADIUS_BLUE * scale * BBSSettings.axesScale.get();
    }

    public static float getRingRadiusRed(float scale)
    {
        return RING_RADIUS_RED * scale * BBSSettings.axesScale.get();
    }

    public static float getRingRadiusGreen(float scale)
    {
        return RING_RADIUS_GREEN * scale * BBSSettings.axesScale.get();
    }

    public static float getRingThickness(float scale)
    {
        return RING_THICKNESS * scale * BBSSettings.axesScale.get();
    }

    public static float getOriginSize(float scale)
    {
        return ORIGIN_SIZE * scale * BBSSettings.axesScale.get();
    }

    public static float getOriginOutline(float scale)
    {
        return ORIGIN_OUTLINE * scale * BBSSettings.axesScale.get();
    }

    public static float getArrowStartOffset(float scale)
    {
        return ARROW_START_OFFSET * scale * BBSSettings.axesScale.get();
    }
}

