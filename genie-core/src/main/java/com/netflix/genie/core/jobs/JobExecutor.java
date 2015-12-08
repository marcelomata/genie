/*
 *
 *  Copyright 2015 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.netflix.genie.core.jobs;

import com.netflix.genie.common.dto.Application;
import com.netflix.genie.common.exceptions.GenieException;
import com.netflix.genie.common.exceptions.GenieServerException;
import com.netflix.genie.core.services.FileCopyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class that contains the logic to setup a genie job and run it.
 *
 * @author amsharma
 * @since 3.0.0
 *
 */
public class JobExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(JobExecutor.class);

    // Directory paths env variables
    private static final String GENIE_WORKING_DIR_ENV_VAR = "GENIE_WORKING_DIR";
    private static final String GENIE_JOB_DIR_ENV_VAR = "GENIE_JOB_DIR";
    private static final String GENIE_CLUSTER_DIR_ENV_VAR = "GENIE_CLUSTER_DIR";
    private static final String GENIE_COMMAND_DIR_ENV_VAR = "GENIE_COMMAND_DIR";
    private static final String GENIE_APPLICATION_DIR_ENV_VAR = "GENIE_APPLICATION_DIR";

    private List<FileCopyService> fileCopyServiceImpls;
//    private String stderrLogPath;
//    private String stdoutLogPath;
//    private String genieLogPath;
//    private String jarsDirectory;


    private final JobExecutionEnvironment jobExecEnv;

    /**
     * Constructor Initialize the object using Job execution environment object.
     *
     * @param fileCopyServiceImpls List of implementations of the file copy interface
     * @param jobExecEnv The job execution environment details like the job, cluster, command and applications.
     */
    public JobExecutor(
            final List<FileCopyService> fileCopyServiceImpls,
            @NotNull(message = "Cannot initialize with null JobExecEnv")
            final JobExecutionEnvironment jobExecEnv) {

        this.fileCopyServiceImpls = fileCopyServiceImpls;
        this.jobExecEnv = jobExecEnv;
    }

    /**
     * Sets up the working directory for the job , ready for execution.
     * This method does not actually run the job.
     */
    public void setup() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("called");
        }
        //setupJobForExecution();
    }

    /**
     * Sets up the working directory for the job and starts the job.
     *
     * @throws GenieException Exception in case of an error
     */
    public void setupAndRun() throws GenieException {
        setupJobForExecution();
        startJob();
    }

    /**
     * Method that setups the job directory and prepares it for execution by creating the runnable script.
     *
     */
    private void setupJobForExecution() throws GenieException {

        setupStandardVariables();
        setupWorkingDirectory();
        processApplications();
        processCommand();
        processCluster();
        processJob();
    };

    /**
     * Method that starts the execution of the job and updates the database with its execution details.
     *
     */
    private void startJob() {

    }

    private void setupStandardVariables() {
        // sets up standard vars like stderr and stdout path cmd.log jars dir, config dir

    }

    /**
     * Method that sets up the current working directory for executing the job.
     */
    private void setupWorkingDirectory() throws GenieException {

        makeDir(jobExecEnv.getJobWorkingDir());
        makeDir(jobExecEnv.getJobWorkingDir() + "/job");
        makeDir(jobExecEnv.getJobWorkingDir() + "/cluster");
        makeDir(jobExecEnv.getJobWorkingDir() + "/command");
        makeDir(jobExecEnv.getJobWorkingDir() + "/applications");
    }

    /**
     * Process all applications content needed for the job to run.
     *
     */
    private void processApplications() throws GenieException {

        for (Application application: this.jobExecEnv.getApplications()) {
            makeDir(jobExecEnv.getJobWorkingDir() + "/applications/" + application.getId());
            //TODO copy down dependencies
        }

    }

    /**
     * Process the command content needed for the job to run.
     *
     */
    private void processCommand() throws GenieException {

        makeDir(jobExecEnv.getJobWorkingDir() + "/command/" + jobExecEnv.getCommand().getId());
        //TODO copy down dependencies

    }

    /**
     * Process the cluster content needed for the job to run.
     *
     */
    private void processCluster() throws GenieException {
        makeDir(jobExecEnv.getJobWorkingDir() + "/cluster/" + jobExecEnv.getCluster().getId());
        //TODO copy down dependencies
    }

    /**
     * Process the Job content needed for the job to run.
     *
     */
    private void processJob() throws GenieException {
        makeDir(jobExecEnv.getJobWorkingDir() + "/job");
        //TODO copy down dependencies
    }

    /**
     * Method to create directories on local unix filesystem.
     *
     * @param dirPath
     * @throws GenieException
     */
    private void makeDir(final String dirPath)
            throws GenieException {
        final List command = new ArrayList<>();
        command.add("mkdir");
        command.add("dirPath");

        executeBashCommand(command);
    }

    /**
     * Method to iterate over a list of fileCopyImpls to copy files.
     *
     * @param src The source path to copy
     * @param dest The destination path to copy
     */
    private void copyFiles(
            final String src,
            final String dest
    ) throws GenieException {

        for (FileCopyService fcs : fileCopyServiceImpls) {
            if (fcs.isValid(src)) {
                fcs.copy(src, dest);
            } else {
                throw new GenieServerException("Genie not equipped to copy down files of this type.");
            }
        }
    }

    /**
     * Helper method that executes a bash command.
     *
     * @param command An array consisting of the command to run
     */
    private void executeBashCommand(final List command) throws GenieException {
        final ProcessBuilder pb = new ProcessBuilder(command);
        try {
            final Process process = pb.start();
            final int errCode = process.waitFor();
            if (errCode != 0) {
                throw new GenieServerException("Unable to execute bash command" + String.valueOf(command));
            }
        } catch (InterruptedException ie) {
            throw new GenieServerException("Unable to execute bash command" +  String.valueOf(command), ie);
        } catch (IOException ioe) {
            throw new GenieServerException("Unable to execute bash command" +  String.valueOf(command), ioe);
        }
    }
}
