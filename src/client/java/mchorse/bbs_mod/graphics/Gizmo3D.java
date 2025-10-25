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
    private static final float AXIS_LENGTH = 1.0F;
    private static final float AXIS_THICKNESS = 0.006F;
    private static final float ARROW_LENGTH = 0.12F;
    private static final float ARROW_WIDTH = 0.03F;
    private static final float RING_RADIUS = 0.4F;
    private static final float RING_THICKNESS = 0.006F;
    private static final float ORIGIN_SIZE = 0.025F;
    private static final float NEG_AXIS_LENGTH = 0.3F;
    private static final float NEG_AXIS_THICKNESS = 0.004F;

    private static final int RING_SEGMENTS = 64;
    private static final int SPHERE_RINGS = 10;
    private static final int SPHERE_SEGMENTS = 12;

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

        renderOriginSphere(stack, scale, alpha);

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

        float axisLen = AXIS_LENGTH * scale;
        float thickness = AXIS_THICKNESS * scale;
        float arrowLen = ARROW_LENGTH * scale;
        float arrowWidth = ARROW_WIDTH * scale;
        float negLen = NEG_AXIS_LENGTH * scale;
        float negThick = NEG_AXIS_THICKNESS * scale;

        renderNegativeAxis(builder, stack, negLen, negThick, r, g, b, alpha * 0.4F);
        renderAxisShaft(builder, stack, axisLen - arrowLen, thickness, r, g, b, alpha);
        renderArrowhead(builder, stack, axisLen - arrowLen, axisLen, arrowWidth, r, g, b, alpha);

        stack.pop();
    }

    private static void renderNegativeAxis(BufferBuilder builder, MatrixStack stack, float length, float thickness, float r, float g, float b, float a)
    {
        Draw.fillBox(builder, stack, -length, -thickness, -thickness, 0, thickness, thickness, r, g, b, a);
    }

    private static void renderAxisShaft(BufferBuilder builder, MatrixStack stack, float length, float thickness, float r, float g, float b, float a)
    {
        Draw.fillBox(builder, stack, 0, -thickness, -thickness, length, thickness, thickness, r, g, b, a);
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
        float radius = RING_RADIUS * scale;
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

        float inner = Math.max(radius - thickness, 0.0001F);
        float outer = radius + thickness;

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

            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, ci0, si0, 0).color(r, g, b, a).next();
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();

            builder.vertex(m, co1, so1, 0).color(r, g, b, a).next();
            builder.vertex(m, ci1, si1, 0).color(r, g, b, a).next();
            builder.vertex(m, co0, so0, 0).color(r, g, b, a).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private static void renderOriginSphere(MatrixStack stack, float scale, float alpha)
    {
        float radius = ORIGIN_SIZE * scale;

        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        builder.begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

        Matrix4f m = stack.peek().getPositionMatrix();

        for (int i = 0; i < SPHERE_RINGS; i++)
        {
            float v0 = (float) i / SPHERE_RINGS;
            float v1 = (float) (i + 1) / SPHERE_RINGS;

            float theta0 = (float) Math.PI * v0 - (float) Math.PI / 2F;
            float theta1 = (float) Math.PI * v1 - (float) Math.PI / 2F;

            float y0 = (float) Math.sin(theta0) * radius;
            float y1 = (float) Math.sin(theta1) * radius;
            float r0 = (float) Math.cos(theta0) * radius;
            float r1 = (float) Math.cos(theta1) * radius;

            for (int j = 0; j < SPHERE_SEGMENTS; j++)
            {
                float u0 = (float) j / SPHERE_SEGMENTS;
                float u1 = (float) (j + 1) / SPHERE_SEGMENTS;

                float phi0 = (float) (2 * Math.PI) * u0;
                float phi1 = (float) (2 * Math.PI) * u1;

                float x00 = (float) Math.cos(phi0) * r0;
                float z00 = (float) Math.sin(phi0) * r0;
                float x01 = (float) Math.cos(phi1) * r0;
                float z01 = (float) Math.sin(phi1) * r0;

                float x10 = (float) Math.cos(phi0) * r1;
                float z10 = (float) Math.sin(phi0) * r1;
                float x11 = (float) Math.cos(phi1) * r1;
                float z11 = (float) Math.sin(phi1) * r1;

                builder.vertex(m, x00, y0, z00).color(1F, 1F, 1F, alpha).next();
                builder.vertex(m, x10, y1, z10).color(1F, 1F, 1F, alpha).next();
                builder.vertex(m, x11, y1, z11).color(1F, 1F, 1F, alpha).next();

                builder.vertex(m, x00, y0, z00).color(1F, 1F, 1F, alpha).next();
                builder.vertex(m, x11, y1, z11).color(1F, 1F, 1F, alpha).next();
                builder.vertex(m, x01, y0, z01).color(1F, 1F, 1F, alpha).next();
            }
        }

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
        return AXIS_LENGTH * scale * BBSSettings.axesScale.get();
    }

    public static float getRingRadius(float scale)
    {
        return RING_RADIUS * scale * BBSSettings.axesScale.get();
    }

    public static float getRingThickness(float scale)
    {
        return RING_THICKNESS * scale * BBSSettings.axesScale.get();
    }

    public static float getAxisThickness(float scale)
    {
        return AXIS_THICKNESS * scale * BBSSettings.axesScale.get();
    }
}

