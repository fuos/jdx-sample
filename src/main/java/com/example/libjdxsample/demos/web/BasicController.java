/**
 * 使用libjdx无头模式打包图集
 * 无图形界面，适合在服务端运行
 */

package com.example.libjdxsample.demos.web;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class BasicController {
    @RequestMapping("/hello")
    @ResponseBody
    public String hello(@RequestParam(name = "name", defaultValue = "unknown user") String name) {
        return "Hello " + name;
    }

    @RequestMapping("/jdx_run")
    @ResponseBody
    public String jdxRun() {
        // 设置目录
        String basicPath = "C:\\workspace\\libjdx-sample\\src\\main\\resources";
        String inDir = basicPath + "in";
        String tmpDir = basicPath + "tmp";
        String replDir = basicPath + "repl";
        String outDir = basicPath + "out";

        // 运行jdx
        long startTime = System.currentTimeMillis();
        runLibgdxApplication(inDir, tmpDir, replDir, outDir);
        long runLibgdxTime = System.currentTimeMillis() - startTime;
        log.info("runLibgdxApplication cost {} ms", runLibgdxTime);
        return "jdx start cost " + runLibgdxTime + " ms";
    }

    // 其实是单独启动了一个服务
    private void runLibgdxApplication(String inDir, String tmpDir, String replDir, String outDir) {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new ReplaceListener(inDir, tmpDir, replDir, outDir), config);
    }
}
