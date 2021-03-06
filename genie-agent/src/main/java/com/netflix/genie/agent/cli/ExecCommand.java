/*
 *
 *  Copyright 2018 Netflix, Inc.
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

package com.netflix.genie.agent.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.netflix.genie.agent.execution.statemachine.JobExecutionStateMachine;
import com.netflix.genie.agent.execution.statemachine.States;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Command to execute a Genie job.
 *
 * @author mprimi
 * @since 4.0.0
 */
@Slf4j
@Component
@Lazy
class ExecCommand implements AgentCommand {

    private final ExecCommandArguments execCommandArguments;
    private final JobExecutionStateMachine stateMachine;

    ExecCommand(
        final ExecCommandArguments execCommandArguments,
        final JobExecutionStateMachine stateMachine
    ) {
        this.execCommandArguments = execCommandArguments;
        this.stateMachine = stateMachine;
    }

    @Override
    public void run() {
        log.info("Running job state machine");
        stateMachine.start();

        final States finalstate;
        try {
            finalstate = stateMachine.waitForStop();
        } catch (final Exception e) {
            log.warn("Job state machine execution failed", e);
            throw new RuntimeException("Job execution error", e);
        }

        if (!States.END.equals(finalstate)) {
            log.warn("Job execution failed with with exception");
            throw new RuntimeException("Job execution failed (final state: {})" + finalstate);
        }

        log.info("Job execution completed");
    }

    @Component
    @Parameters(commandNames = CommandNames.EXEC, commandDescription = "Execute a Genie job")
    @Getter
    static class ExecCommandArguments implements AgentCommandArguments {
        @Parameter(names = "timeout", description = "Job execution timeout")
        private int jobTimeout = 2000;

        @ParametersDelegate
        private final ArgumentDelegates.ServerArguments serverArguments;

        @ParametersDelegate
        private final ArgumentDelegates.CacheArguments cacheArgumentsArguments;

        ExecCommandArguments(
            final ArgumentDelegates.ServerArguments serverArguments,
            final ArgumentDelegates.CacheArguments cacheArgumentsArguments
        ) {
            this.serverArguments = serverArguments;
            this.cacheArgumentsArguments = cacheArgumentsArguments;
        }

        @Override
        public Class<? extends AgentCommand> getConsumerClass() {
            return ExecCommand.class;
        }
    }
}
