package cn.blockforge.generated.mode539c988.client;

import cn.blockforge.generated.mode539c988.GeneratedMod;
import cn.blockforge.generated.mode539c988.entity.TrainEntity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TrainRenderer extends EntityRenderer<TrainEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            GeneratedMod.MOD_ID, "textures/entity/monochrome_commuter_train_projected_texture_fixed.png");
    private static final ResourceLocation MODEL = ResourceLocation.fromNamespaceAndPath(
            GeneratedMod.MOD_ID, "models/entity/monochrome_commuter_train_projected_fixed.json");
    private static final float TEXTURE_WIDTH = 2048.0F;
    private static final float TEXTURE_HEIGHT = 1664.0F;

    private final List<Cube> cubes = new ArrayList<>();

    public TrainRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.6F;
        loadOriginalModel(context);
    }

    private void loadOriginalModel(EntityRendererProvider.Context context) {
        try (BufferedReader reader = context.getResourceManager().openAsReader(MODEL)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("elements")) {
                JsonObject object = element.getAsJsonObject();
                if (!"cube".equals(object.get("type").getAsString())) {
                    continue;
                }
                // The baked side texture already contains all windows and doors.
                // Old projected overlay cubes sampled misaligned strips of the atlas;
                // omitting only these duplicates fixes the displaced window/door facade.
                String partName = object.has("name") ? object.get("name").getAsString() : "";
                if (partName.startsWith("L window ") || partName.startsWith("R window ")
                        || partName.startsWith("L door ") || partName.startsWith("R door ")) {
                    continue;
                }
                float[] from = vector(object.getAsJsonArray("from"));
                float[] to = vector(object.getAsJsonArray("to"));
                JsonObject facesObject = object.getAsJsonObject("faces");
                List<Face> faces = new ArrayList<>();
                for (Map.Entry<String, JsonElement> faceEntry : facesObject.entrySet()) {
                    JsonArray uv = faceEntry.getValue().getAsJsonObject().getAsJsonArray("uv");
                    faces.add(new Face(faceEntry.getKey(), uv.get(0).getAsFloat(), uv.get(1).getAsFloat(),
                            uv.get(2).getAsFloat(), uv.get(3).getAsFloat()));
                }
                cubes.add(new Cube(from, to, faces));
            }
        } catch (Exception exception) {
            throw new IllegalStateException("无法读取原始电车模型 " + MODEL, exception);
        }
    }

    private static float[] vector(JsonArray array) {
        return new float[]{array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()};
    }

    @Override
    public void render(TrainEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw - 90.0F));
        if (entity.isBossTrain() && entity.getBossSpinAngle() != 0.0F)
            poseStack.mulPose(Axis.YP.rotationDegrees(entity.getBossSpinAngle()));
        poseStack.translate(0.0D, 0.24D, 0.0D);
        float modelScale = entity.isBossTrain() ? 4.0F : entity.isSkyfall() ? 5.0F
                : entity.getMode() == TrainEntity.Mode.DRIVABLE ? 1.35F : 1.0F;
        poseStack.scale(modelScale / 16.0F, modelScale / 16.0F, modelScale / 16.0F);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        PoseStack.Pose pose = poseStack.last();
        for (Cube cube : cubes) {
            renderCube(cube, pose, consumer, packedLight);
        }
        poseStack.popPose();
        // Boss name is displayed by the genuine boss health bar, never inside its giant mesh.
        if (!entity.isBossTrain())
            super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static void renderCube(Cube cube, PoseStack.Pose pose, VertexConsumer consumer, int packedLight) {
        float x0 = cube.from[0];
        float y0 = cube.from[1];
        float z0 = cube.from[2];
        float x1 = cube.to[0];
        float y1 = cube.to[1];
        float z1 = cube.to[2];
        for (Face face : cube.faces) {
            switch (face.direction) {
                case "north" -> quad(pose, consumer, packedLight, face,
                        x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0, 0, -1);
                case "south" -> quad(pose, consumer, packedLight, face,
                        x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0, 0, 1);
                case "west" -> quad(pose, consumer, packedLight, face,
                        x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1, 0, 0);
                case "east" -> quad(pose, consumer, packedLight, face,
                        x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1, 0, 0);
                case "up" -> quad(pose, consumer, packedLight, face,
                        x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0, 1, 0);
                case "down" -> quad(pose, consumer, packedLight, face,
                        x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0);
                default -> { }
            }
        }
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer consumer, int light, Face uv,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz) {
        Matrix4f matrix = pose.pose();
        vertex(consumer, pose, matrix, ax, ay, az, uv.u1, uv.v2, light, nx, ny, nz);
        vertex(consumer, pose, matrix, bx, by, bz, uv.u2, uv.v2, light, nx, ny, nz);
        vertex(consumer, pose, matrix, cx, cy, cz, uv.u2, uv.v1, light, nx, ny, nz);
        vertex(consumer, pose, matrix, dx, dy, dz, uv.u1, uv.v1, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f matrix,
                               float x, float y, float z, float u, float v, int light,
                               float nx, float ny, float nz) {
        consumer.addVertex(matrix, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u / TEXTURE_WIDTH, v / TEXTURE_HEIGHT)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    @Override
    public ResourceLocation getTextureLocation(TrainEntity entity) {
        return TEXTURE;
    }

    private record Cube(float[] from, float[] to, List<Face> faces) { }
    private record Face(String direction, float u1, float v1, float u2, float v2) { }
}
