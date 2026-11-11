
package com.queuepilot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JobControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JobRepository jobRepository;

    @Test
    void createJobReturnsCreatedResponse() throws Exception {

        MvcResult result = mockMvc.perform(
                post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jobType": "API_TEST",
                                    "payload": {
                                        "input": "hello"
                                    },
                                    "maxAttempts": 3
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(jsonPath("$.jobType").value("API_TEST"))
                .andExpect(jsonPath("$.status").value("QUEUED"))
                .andExpect(jsonPath("$.maxAttempts").value(3))
                .andReturn();

        String location = result.getResponse().getHeader("Location");

        assertNotNull(location);
        assertTrue(location.startsWith("/api/jobs/"));
    }

    @Test
    void createJobUsesDefaultMaxAttemptsWhenNotProvided()
            throws Exception {

        mockMvc.perform(
                post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jobType": "API_DEFAULT_TEST",
                                    "payload": {
                                        "input": "hello"
                                    }
                                }
                                """)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.maxAttempts").value(3));
    }

    @Test
    void getJobsReturnsListContainingSavedJob() throws Exception {

        Job job = createAndSaveJob("API_LIST_TEST");

        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                job.getId().toString()
                        )
                ));
    }

    @Test
    void getJobReturnsRequestedJob() throws Exception {

        Job job = createAndSaveJob("API_GET_TEST");

        mockMvc.perform(get("/api/jobs/{id}", job.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(
                        job.getId().toString()
                ))
                .andExpect(jsonPath("$.jobType").value("API_GET_TEST"));
    }

    @Test
    void getJobReturnsNotFoundForUnknownId() throws Exception {

        UUID unknownId = UUID.randomUUID();

        mockMvc.perform(get("/api/jobs/{id}", unknownId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createJobRejectsBlankJobType() throws Exception {

        mockMvc.perform(
                post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jobType": "   ",
                                    "payload": {},
                                    "maxAttempts": 3
                                }
                                """)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void createJobRejectsMaxAttemptsAboveLimit() throws Exception {

        mockMvc.perform(
                post("/api/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "jobType": "API_TEST",
                                    "payload": {},
                                    "maxAttempts": 11
                                }
                                """)
        )
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthEndpointReturnsUpStatus() throws Exception {

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("QueuePilot API"));
    }

    private Job createAndSaveJob(String jobType) {

        Job job = new Job();
        job.setJobType(jobType);

        return jobRepository.saveAndFlush(job);
    }
}
