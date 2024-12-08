package com.example.libjdxsample.demos.web;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

@Slf4j
public class ReplaceListener extends ApplicationAdapter {

    public static final String PNG = ".png";
    public static final String ATLAS = ".atlas";
    private static final int MAX_WIDTH = 2048;
    private static final int MAX_HEIGHT = 2048;
    private final String inDir;
    private final String tmpDir;
    private final String replDir;
    private final String outDir;

    public ReplaceListener(String inDir, String tmpDir, String replDir, String outDir) {
        this.inDir = inDir;
        this.tmpDir = tmpDir;
        this.replDir = replDir;
        this.outDir = outDir;
    }

    @Override
    public void create() {
        try {
            initializeGdx();
            processTextures();
        } catch (Exception e) {
            log.error("Error processing textures", e);
        } finally {
            Gdx.app.exit();
        }
    }

    // 初始化GL
    private void initializeGdx() {
        log.info("Initializing Gdx");
        Gdx.gl = new MockGL20();
        log.info("Gdx initialized");
    }

    private void processTextures() throws IOException {
        File inDir = new File(this.inDir);
        String atlasName = getAtlasName(inDir);
        if (atlasName == null) {
            throw new IOException("No atlas file found in /in directory");
        }

        TextureAtlas atlas = new TextureAtlas(Gdx.files.absolute(this.inDir + "/" + atlasName + ATLAS));
        File tmpDir = new File(this.tmpDir);
        boolean created = tmpDir.mkdirs();
        if (!created) {
            throw new IOException("Failed to create /tmp directory");
        }

        long startTime = System.currentTimeMillis();
        for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
            Pixmap pixmap = extractPixmap(region);
            PixmapIO.writePNG(Gdx.files.absolute(this.tmpDir + "/" + region.name + PNG), pixmap);
            pixmap.dispose();
        }
        long pixmapTime = System.currentTimeMillis() - startTime;
        log.info("Pixmap convert cost {} ms", pixmapTime);

        startTime = System.currentTimeMillis();
        File replDir = new File(this.replDir);
        int count = 0;
        for (File file : Objects.requireNonNull(replDir.listFiles())) {
            if (file.getName().endsWith(PNG)) {
                String fileName = file.getName();
                File matchingFile = findFileInDirectory(new File(this.tmpDir), fileName);
                if (matchingFile != null) {
                    Pixmap newPixmap = new Pixmap(Gdx.files.internal(file.getAbsolutePath()));
                    PixmapIO.writePNG(Gdx.files.absolute(matchingFile.getAbsolutePath()), newPixmap);
                    newPixmap.dispose();
                    count++;
                }
            }
        }
        long copyTime = System.currentTimeMillis() - startTime;
        log.info("Copy files cost {} ms, count: {}", copyTime, count);

        // 打包参数
        TexturePacker.Settings settings = new TexturePacker.Settings();
        settings.silent = true;
        settings.maxWidth = MAX_WIDTH;
        settings.maxHeight = MAX_HEIGHT;
        settings.filterMin = Texture.TextureFilter.Linear;
        settings.filterMag = Texture.TextureFilter.Linear;
        settings.combineSubdirectories = true;
        settings.premultiplyAlpha = true;
        settings.useIndexes = false;
        settings.alphaThreshold = 3;
        // 开始打包
        TexturePacker.process(settings, this.tmpDir, outDir, atlasName);
        deleteDirectory(tmpDir);
    }

    private String getAtlasName(File dir) {
        return Arrays.stream(Objects.requireNonNull(dir.listFiles()))
                .filter(file -> file.getName().endsWith(ATLAS))
                .findFirst()
                .map(file -> file.getName().replace(ATLAS, ""))
                .orElse(null);
    }

    // 递归搜索目录
    private File findFileInDirectory(File directory, String targetFileName) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File result = findFileInDirectory(file, targetFileName);
                    if (result != null) {
                        return result;
                    }
                } else if (file.getName().equals(targetFileName)) {
                    return file;
                }
            }
        }
        return null;
    }

    private Pixmap extractPixmap(TextureAtlas.AtlasRegion region) {
        TextureRegion textureRegion = new TextureRegion(region);
        Texture texture = textureRegion.getTexture();
        if (!texture.getTextureData().isPrepared()) {
            texture.getTextureData().prepare();
        }
        Pixmap pixmap = texture.getTextureData().consumePixmap();
        Pixmap result = new Pixmap(region.getRegionWidth(), region.getRegionHeight(), Pixmap.Format.RGBA8888);

        for (int x = 0; x < region.getRegionWidth(); x++) {
            for (int y = 0; y < region.getRegionHeight(); y++) {
                int colorInt = pixmap.getPixel(region.getRegionX() + x, region.getRegionY() + y);
                int alpha = (colorInt >> 24) & 0xFF;
                int rgb = colorInt & 0xFFFFFF;
                result.drawPixel(x, y, (alpha << 24) | rgb);
            }
        }

        return result;
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    boolean deletedFile = file.delete();
                    if (!deletedFile) {
                        log.error("Failed to delete file {}", file);
                    }
                }
            }
        }
        boolean deletedDir = directory.delete();
        if (!deletedDir) {
            log.error("Failed to delete directory {}", directory);
        }
    }
}
