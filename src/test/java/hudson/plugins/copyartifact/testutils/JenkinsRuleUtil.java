/*
 * The MIT License
 *
 * Copyright (c) 2017 IKEDA Yasuyuki
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package hudson.plugins.copyartifact.testutils;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jvnet.hudson.test.ExtractResourceSCM;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.scm.SCM;

/**
 *
 */
public class JenkinsRuleUtil {

    private JenkinsRuleUtil() {
    }

    /**
     * Get Web Client that allows 405 Method Not Allowed.
     * This happens when accessing build page of a project with parameters.
     */
    public static JenkinsRule.WebClient createAllow405WebClient(JenkinsRule rule) {
        JenkinsRule.WebClient webClient = rule.createWebClient();
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setFetchPolyfillEnabled(true);
        return webClient;
    }

    /**
     * Creates a new workflow (aka. pipeline) job
     *
     * @param name the new job name
     * @param script the script put in <code>node{}</code>
     * @return the new workflow job
     * @throws Exception an error when creating a new job
     */
    public static WorkflowJob createWorkflow(JenkinsRule rule, String name, String script) throws Exception {
        WorkflowJob job = rule.createProject(WorkflowJob.class, name);
        job.setDefinition(new CpsFlowDefinition("node {" + script + "}", true));
        return job;
    }

    /**
     * Create SCM from the specified directory in resources.
     *
     * @param tempFolder an instance of {@link File}
     * @param resource URL for the directory gotten with {@link Class#getResource(String)}
     * @return SCM
     * @throws Exception
     */
    public static SCM getExtractResourceScm(File tempFolder, URL resource) throws Exception {
        File scmZip = File.createTempFile("junit", null, tempFolder);
        final Path scmSource = Paths.get(resource.toURI());
        URI scmZipUri = new URI("jar", scmZip.toURI().toString(), null);
        Files.deleteIfExists(scmZip.toPath());
        Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        try(final FileSystem zipFile = FileSystems.newFileSystem(scmZipUri, env)) {
            Files.walkFileTree(scmSource, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attrs
                ) throws IOException {
                    Path dest = zipFile.getPath(
                            "/",
                            scmSource.relativize(file).toString()
                    );
                    Files.copy(file, dest);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(
                        Path dir,
                        BasicFileAttributes attrs
                ) throws IOException {
                    Path dest = zipFile.getPath(
                            "/",
                            scmSource.relativize(dir).toString()
                    );
                    if (Files.notExists(dest)) {
                        // Creating root directory cause FileAlreadyExistsException
                        Files.createDirectory(dest);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return new ExtractResourceSCM(scmZip.toURI().toURL());
        }
    }
}
