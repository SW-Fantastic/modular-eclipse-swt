package org.swdc.swt.plugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.eclipse.sisu.space.asm.*;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@Mojo(name = "repackage")
public class SWTRepackageMojo extends AbstractMojo {

    @Inject
    private MavenProject project;

    @Inject
    private RepositorySystem system;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        String swtGroupId = "org.eclipse.platform";
        String swtWindows = "org.eclipse.swt.win32.win32.x86_64";
        String swtMacos = "org.eclipse.swt.cocoa.macosx.x86_64";
        String swtLinux = "org.eclipse.swt.gtk.linux.x86_64";

        getLog().info("Repackage SWT libraries");

        Map<String, File> packages = new HashMap<>();

        // 在依赖中查找SWT的Platform库
        for (Dependency dependency : project.getDependencies()) {
            if (dependency.getGroupId().equals(swtGroupId)) {

                Artifact artifact = system.createDependencyArtifact(dependency);
                ArtifactResolutionRequest resolutionRequest = new ArtifactResolutionRequest();
                resolutionRequest.setArtifact(artifact);
                ArtifactResolutionResult resolved = system.resolve(resolutionRequest);
                for (Artifact resolve: resolved.getArtifacts()) {
                    if (resolve.getArtifactId().equals(swtWindows)) {
                        packages.put(swtWindows,resolve.getFile());
                    } else if (resolve.getArtifactId().equals(swtLinux)) {
                        packages.put(swtLinux,resolve.getFile());
                    } else if (resolve.getArtifactId().equals(swtMacos)) {
                        packages.put(swtMacos,resolve.getFile());
                    }
                }

            }
        }


        List<String> exportList = new ArrayList<>();
        File output = new File(project.getBuild().getOutputDirectory());
        if (packages.containsKey(swtMacos)) {
            // 重新打包macos的SWT
            extractFile(output,packages.get(swtMacos),exportList);
        }

        if (packages.containsKey(swtWindows)) {
            // 重新打包Windows的SWT
            extractFile(output,packages.get(swtWindows),exportList);
        }

        if (packages.containsKey(swtLinux)) {
            // 重新打包Linux的SWT
            extractFile(output,packages.get(swtLinux),exportList);
        }

        // 生成Module-info
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V9,Opcodes.ACC_MODULE,"module-info",null,null,null);
        ModuleVisitor module = writer.visitModule("swdc.eclipse.swt",0,null);
        module.visitRequire("java.base",0,null);
        for (String pack: exportList) {
            module.visitExport(pack,0);
        }

        try {
            // 输出Module-info
            File moduleInfo = new File(output,"module-info.class");
            FileOutputStream os = new FileOutputStream(moduleInfo);
            os.write(writer.toByteArray());
            os.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * 重打包一个jar
     * @param outPath 输出路径
     * @param jar 依赖的jar文件
     * @param exportList 导出列表，用于生成module-info
     */
    private void extractFile(File outPath, File jar, List<String> exportList) {
        try {


            ZipFile zin = new ZipFile(jar);
            Enumeration<? extends ZipEntry> entry = zin.entries();
            while (entry.hasMoreElements()) {

                ZipEntry item = entry.nextElement();

                String itemName = item.getName();
                if (item.getName().contains("/")) {
                    itemName = item.getName().substring(item.getName().lastIndexOf("/"));
                }


                if (!itemName.endsWith("class")) {

                    if (item.isDirectory() || item.getName().endsWith("RSA") || item.getName().endsWith("SF")) {
                        continue;
                    }

                    File base = new File(outPath,item.getName()).getParentFile();
                    if (!base.exists()) {
                        base.mkdirs();
                    }

                    InputStream in = zin.getInputStream(item);
                    FileOutputStream fos = new FileOutputStream(new File(base,itemName));
                    in.transferTo(fos);
                    in.close();
                    fos.close();

                    continue;
                }

                InputStream inputStream = zin.getInputStream(item);
                ClassReader reader = new ClassReader(inputStream);
                ClassWriter writer = new ClassWriter(reader,0);

                reader.accept(writer,0);

                String className = reader.getClassName();
                String packageName = "/" + className.substring(0,className.lastIndexOf("/"));

                String exportPackage = packageName.substring(1);
                if (!exportList.contains(exportPackage)) {
                    exportList.add(exportPackage);
                }
                File folderPackage = new File(outPath,packageName);
                if (!folderPackage.exists()) {
                    folderPackage.mkdirs();
                }

                File targetFile = new File(folderPackage,itemName);
                FileOutputStream fos = new FileOutputStream(targetFile);
                fos.write(writer.toByteArray());
                fos.close();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * 合并多个jar包，并且修改它的package。
     * 这个方法没有使用，放这里仅仅用于参考，未来其他项目或许需要。
     *
     * @param outPath 输出路径，Maven引擎提供。
     * @param jar 依赖库的jar包
     * @param oldPackage 被替换的package字符串
     * @param newPackage 新的package字符串
     * @param exportList 导出列表，用于写模块描述符（module-info）
     */
    private void extractFile(File outPath, File jar, String oldPackage, String newPackage, List<String> exportList) {
        try {

            String originPackageDeclare = oldPackage.replace(".", "/");
            String targetPackageDeclare = newPackage.replace(".", "/");

            ZipFile zin = new ZipFile(jar);
            Enumeration<? extends ZipEntry> entry = zin.entries();
            while (entry.hasMoreElements()) {

                ZipEntry item = entry.nextElement();

                String itemName = item.getName();
                if (item.getName().contains("/")) {
                    itemName = item.getName().substring(item.getName().lastIndexOf("/"));
                }

                File target = new File(outPath, "/" + newPackage.replace(".", "/"));
                if (!target.exists()) {
                    target.mkdirs();
                }

                if (!itemName.endsWith("class")) {

                    if (item.isDirectory() || item.getName().endsWith("RSA") || item.getName().endsWith("SF")) {
                        continue;
                    }

                    File base = new File(outPath,item.getName()).getParentFile();
                    if (!base.exists()) {
                        base.mkdirs();
                    }

                    InputStream in = zin.getInputStream(item);
                    FileOutputStream fos = new FileOutputStream(new File(base,itemName));
                    in.transferTo(fos);
                    in.close();
                    fos.close();

                    continue;
                }

                InputStream inputStream = zin.getInputStream(item);
                ClassReader reader = new ClassReader(inputStream);
                ClassWriter writer = new ClassWriter(reader,0);
                RelocatePackageVisitor visitor = new RelocatePackageVisitor(Opcodes.ASM9,writer,oldPackage,newPackage);

                reader.accept(visitor,0);
                String className = reader.getClassName();
                String packageName = "/" + className.substring(0,className.lastIndexOf("/"));

                packageName = packageName.replace(originPackageDeclare,targetPackageDeclare);

                String exportPackage = packageName.substring(1);
                if (!exportList.contains(exportPackage)) {
                    exportList.add(exportPackage);
                }
                File folderPackage = new File(outPath,packageName);
                if (!folderPackage.exists()) {
                    folderPackage.mkdirs();
                }

                File targetFile = new File(folderPackage,itemName);
                FileOutputStream fos = new FileOutputStream(targetFile);
                fos.write(writer.toByteArray());
                fos.close();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
