package efs.task.todoapp.autograding;

import efs.task.todoapp.util.ToDoServerExtension;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static efs.task.todoapp.autograding.TestUtils.HEADER_AUTH;
import static efs.task.todoapp.autograding.TestUtils.taskDeleteArguments;
import static efs.task.todoapp.autograding.TestUtils.taskGetArguments;
import static efs.task.todoapp.autograding.TestUtils.taskJson;
import static efs.task.todoapp.autograding.TestUtils.taskPostArguments;
import static efs.task.todoapp.autograding.TestUtils.taskPutArguments;
import static efs.task.todoapp.autograding.TestUtils.taskRequestBuilder;
import static efs.task.todoapp.autograding.TestUtils.userJson;
import static efs.task.todoapp.autograding.TestUtils.userRequestBuilder;
import static efs.task.todoapp.autograding.TestUtils.validUUID;
import static efs.task.todoapp.autograding.TestUtils.wrongCodeMessage;
import static efs.task.todoapp.autograding.HttpResonseStatus.BAD_REQUEST;
import static efs.task.todoapp.autograding.HttpResonseStatus.CREATED;
import static efs.task.todoapp.autograding.HttpResonseStatus.FORBIDDEN;
import static efs.task.todoapp.autograding.HttpResonseStatus.NOT_FOUND;
import static efs.task.todoapp.autograding.HttpResonseStatus.OK;
import static efs.task.todoapp.autograding.HttpResonseStatus.UNAUTHORIZED;
import static java.net.http.HttpRequest.BodyPublishers.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;
import static org.skyscreamer.jsonassert.JSONCompareMode.STRICT;

@ExtendWith(ToDoServerExtension.class)
class TaskEndpointTest {

    private static final UUID ID = UUID.randomUUID();
    private static final String BASE_64_USERNAME = "dXNlcm5hbWU=";
    private static final String BASE_64_USERNAME_2 = "dXNlcm5hbWUy";
    private static final String BASE_64_PASSWORD = "cGFzc3dvcmQ=";
    private static final String BASE_64_WRONG_PASSWORD = "d3JvbmdQYXNzd29yZA==";

    private HttpClient httpClient;

    @BeforeEach
    void setUp() {
        httpClient = HttpClient.newHttpClient();
    }

  private static Stream<Arguments> authHeaderBadRequestsPost() {
    return Stream.of(
            taskPostArguments(null, taskJson("task")),
            taskPostArguments(BASE_64_USERNAME, taskJson("task")),
            taskPostArguments("dummy:" + BASE_64_PASSWORD, taskJson("task")),
            taskPostArguments(BASE_64_USERNAME + ":dummy", taskJson("task")))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing auth header", objects[0], objects[1]));
  }

  private static Stream<Arguments> authHeaderBadRequestsGet() {
    return Stream.of(
            taskGetArguments(null),
            taskGetArguments(BASE_64_USERNAME),
            taskGetArguments("dummy:" + BASE_64_PASSWORD),
            taskGetArguments(BASE_64_USERNAME + ":dummy"))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing auth header", objects[0], objects[1]));
  }

  private static Stream<Arguments> authHeaderBadRequestsPut() {
    return Stream.of(
            taskPutArguments(null, taskJson("task")),
            taskPutArguments(BASE_64_USERNAME, taskJson("task")),
            taskPutArguments("dummy:" + BASE_64_PASSWORD, taskJson("task")),
            taskPutArguments(BASE_64_USERNAME + ":dummy", taskJson("task")))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing auth header", objects[0], objects[1]));
  }

  private static Stream<Arguments> authHeaderBadRequestsDelete() {
    return Stream.of(
            taskDeleteArguments(null),
            taskDeleteArguments(BASE_64_USERNAME),
            taskDeleteArguments("dummy:" + BASE_64_PASSWORD),
            taskDeleteArguments(BASE_64_USERNAME + ":dummy"))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing auth header", objects[0], objects[1]));
  }

  private static Stream<Arguments> taskBodyBadRequestsPost() {
    return Stream.of(
            taskPostArguments(BASE_64_USERNAME + ":" + BASE_64_PASSWORD, ""),
            taskPostArguments(BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson(null)),
            taskPostArguments(
                BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson("buy milk", "not-a-date")))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing task body", objects[0], objects[1]));
  }

  private static Stream<Arguments> taskBodyBadRequestsPut() {
    return Stream.of(
            taskPutArguments(BASE_64_USERNAME + ":" + BASE_64_PASSWORD, ""),
            taskPutArguments(BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson(null)),
            taskPutArguments(
                BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson("buy milk", "not-a-date")))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing task body", objects[0], objects[1]));
  }

  private static Stream<Arguments> taskPathBadRequestsGet() {
    return Stream.of(taskGetArguments("123", BASE_64_USERNAME + ":" + BASE_64_PASSWORD))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing task path", objects[0], objects[1]));
  }

  private static Stream<Arguments> taskPathBadRequestsPut() {
    return Stream.of(
            taskPutArguments(BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson("task")),
            taskPutArguments("123", BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson("task")))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing task path", objects[0], objects[1]));
  }

  private static Stream<Arguments> taskPathBadRequestsDelete() {
    return Stream.of(
            taskDeleteArguments(BASE_64_USERNAME + ":" + BASE_64_PASSWORD),
            taskDeleteArguments("123", BASE_64_USERNAME + ":" + BASE_64_PASSWORD))
        .map(Arguments::get)
        .map(objects -> Arguments.of("Wrong or missing task path", objects[0], objects[1]));
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"taskPathBadRequestsGet"})
  @Timeout(1)
  void shouldReturnBadRequestStatusIncorrectTaskPathGet(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(BAD_REQUEST.getCode());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"taskPathBadRequestsPut"})
  @Timeout(1)
  void shouldReturnBadRequestStatusIncorrectTaskPathPut(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(BAD_REQUEST.getCode());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"taskPathBadRequestsDelete"})
  @Timeout(1)
  void shouldReturnBadRequestStatusIncorrectTaskPathDelete(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(BAD_REQUEST.getCode());
  }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"taskBodyBadRequestsPost"})
  @Timeout(1)
  void shouldReturnBadRequestStatusIncorrectTaskBodyPost(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(BAD_REQUEST.getCode());
  }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"taskBodyBadRequestsPut"})
    @Timeout(1)
    void shouldReturnBadRequestStatusIncorrectTaskBodyPut(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(BAD_REQUEST.getCode());
    }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"authHeaderBadRequestsPost"})
  @Timeout(1)
  void shouldReturnBadRequestStatusForIncorrectAuthenticationHeadersPost(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(BAD_REQUEST.getCode());
  }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"authHeaderBadRequestsGet"})
    @Timeout(1)
    void shouldReturnBadRequestStatusForIncorrectAuthenticationHeadersGet(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"authHeaderBadRequestsPut"})
    @Timeout(1)
    void shouldReturnBadRequestStatusForIncorrectAuthenticationHeadersPut(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(BAD_REQUEST.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"authHeaderBadRequestsDelete"})
    @Timeout(1)
    void shouldReturnBadRequestStatusForIncorrectAuthenticationHeadersDelete(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(BAD_REQUEST.getCode());
    }

    private static Stream<Arguments> wrongUsernameUnauthorizedRequestsGet() {
        return Stream.of(
                taskGetArguments(BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD),
                taskGetArguments(ID, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
        ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong username", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongUsernameUnauthorizedRequestsPost() {
        return Stream.of(
                        taskPostArguments(BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD, taskJson("task"))
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong username", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongUsernameUnauthorizedRequestsPut() {
        return Stream.of(
                        taskPutArguments(ID, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD, taskJson("task"))
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong username", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongUsernameUnauthorizedRequestsDelete() {
        return Stream.of(
                        taskDeleteArguments(ID, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong username", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongPasswordUnauthorizedRequestsGet() {
        return Stream.of(
                taskGetArguments(BASE_64_USERNAME_2 + ":" + BASE_64_WRONG_PASSWORD),
                taskGetArguments(ID, BASE_64_USERNAME_2 + ":" + BASE_64_WRONG_PASSWORD)
        ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong password", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongPasswordUnauthorizedRequestsPost() {
        return Stream.of(
                        taskPostArguments(BASE_64_USERNAME + ":" + BASE_64_WRONG_PASSWORD, taskJson("task"))
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong password", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongPasswordUnauthorizedRequestsPut() {
        return Stream.of(
                        taskPutArguments(ID, BASE_64_USERNAME_2 + ":" + BASE_64_WRONG_PASSWORD, taskJson("task"))
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong password", objects[0], objects[1]));
    }

    private static Stream<Arguments> wrongPasswordUnauthorizedRequestsDelete() {
        return Stream.of(
                        taskDeleteArguments(ID, BASE_64_USERNAME_2 + ":" + BASE_64_WRONG_PASSWORD)
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Wrong password", objects[0], objects[1]));
    }

    private static Stream<HttpRequest.Builder> unauthorizedRequests() {
        return Stream.of(
                // Wrong username
                taskRequestBuilder()
                        .header(HEADER_AUTH, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                        .POST(ofString(taskJson("task"))),
                taskRequestBuilder()
                        .header(HEADER_AUTH, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                        .GET(),
                taskRequestBuilder(ID)
                        .header(HEADER_AUTH, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                        .GET(),
                taskRequestBuilder(ID)
                        .header(HEADER_AUTH, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                        .PUT(ofString(taskJson("task"))),
                taskRequestBuilder(ID)
                        .header(HEADER_AUTH, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                        .DELETE(),

                //Wrong password
                taskRequestBuilder()
                        .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_WRONG_PASSWORD)
                        .POST(ofString(taskJson("task"))),
                taskRequestBuilder()
                        .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_WRONG_PASSWORD)
                        .GET(),
                taskRequestBuilder(ID)
                        .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_WRONG_PASSWORD)
                        .GET(),
                taskRequestBuilder(ID)
                        .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_WRONG_PASSWORD)
                        .PUT(ofString(taskJson("task"))),
                taskRequestBuilder(ID)
                        .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_WRONG_PASSWORD)
                        .DELETE()
        );
    }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"wrongUsernameUnauthorizedRequestsGet"})
  @Timeout(1)
  void shouldReturnUnauthorizedStatusWrongUsernameGet(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(UNAUTHORIZED.getCode());
  }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"wrongUsernameUnauthorizedRequestsPost"})
    @Timeout(1)
    void shouldReturnUnauthorizedStatusWrongUsernamePost(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(UNAUTHORIZED.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"wrongUsernameUnauthorizedRequestsPut"})
    @Timeout(1)
    void shouldReturnUnauthorizedStatusWrongUsernamePut(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(UNAUTHORIZED.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"wrongUsernameUnauthorizedRequestsDelete"})
    @Timeout(1)
    void shouldReturnUnauthorizedStatusWrongUsername(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(UNAUTHORIZED.getCode());
    }

  @ParameterizedTest(name = "{1}")
  @MethodSource({"wrongPasswordUnauthorizedRequestsGet"})
  @Timeout(1)
  void shouldReturnUnauthorizedStatusWrongPasswordGet(
      String testPrefix, String testName, HttpRequest taskRequest)
      throws IOException, InterruptedException {
    // given
    var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
    httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

    // when
    var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

    // then
    assertThat(httpResponse.statusCode())
        .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
        .isEqualTo(UNAUTHORIZED.getCode());
  }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"wrongPasswordUnauthorizedRequestsPost"})
    @Timeout(1)
    void shouldReturnUnauthorizedStatusWrongPasswordPost(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(UNAUTHORIZED.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"wrongPasswordUnauthorizedRequestsPut"})
    @Timeout(1)
    void shouldReturnUnauthorizedStatusWrongPasswordPut(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(UNAUTHORIZED.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource({"wrongPasswordUnauthorizedRequestsDelete"})
    @Timeout(1)
    void shouldReturnUnauthorizedStatusWrongPasswordDelete(
            String testPrefix, String testName, HttpRequest taskRequest)
            throws IOException, InterruptedException {
        // given
        var userRequest = userRequestBuilder().POST(ofString(userJson("username", "password"))).build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        // when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        // then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(UNAUTHORIZED.getCode());
    }


    @Test
    @Timeout(1)
    void shouldCreateTask() throws IOException, InterruptedException, JSONException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        var postTaskRequest = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME +
                        ":" +
                        BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();

        //when
        var postTaskResponse = httpClient.send(postTaskRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertAll(
                () -> assertThat(postTaskResponse.statusCode()).as(wrongCodeMessage(postTaskRequest)).isEqualTo(CREATED.getCode()),
                () -> {
                    var jsonResponse = getJsonFromResponse(postTaskResponse);
                    assertThat(jsonResponse.getString("id")).as("Wrong task identifier in response").is(validUUID());
                }
        );
    }

    @Test
    @Timeout(1)
    void shouldReturnAllTasksForUser() throws IOException, InterruptedException, JSONException {
        //given
        var user1Request = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(user1Request, HttpResponse.BodyHandlers.ofString());

        var user2Request = userRequestBuilder()
                .POST(ofString(userJson("username2", "password")))
                .build();
        httpClient.send(user2Request, HttpResponse.BodyHandlers.ofString());

        var postTaskForUser1Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME +
                        ":" +
                        BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskForUser1RequestResponse = httpClient.send(postTaskForUser1Request, HttpResponse.BodyHandlers.ofString());
        var task1Id = getIdOfCreatedTask(postTaskForUser1RequestResponse);

        postTaskForUser1Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME +
                        ":" +
                        BASE_64_PASSWORD)
                .POST(ofString(taskJson("eat an apple")))
                .build();
        postTaskForUser1RequestResponse = httpClient.send(postTaskForUser1Request, HttpResponse.BodyHandlers.ofString());
        var task2Id = getIdOfCreatedTask(postTaskForUser1RequestResponse);

        var postTaskForUser2Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        httpClient.send(postTaskForUser2Request, HttpResponse.BodyHandlers.ofString());

        var getForUser1Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME +
                        ":" +
                        BASE_64_PASSWORD)
                .GET()
                .build();

        //when
        var getForUser1Response = httpClient.send(getForUser1Request, HttpResponse.BodyHandlers.ofString());

        //then
        var jsonTasks = new JSONArray(getForUser1Response.body());
        var sortedTasks = sortJsonTasks(task1Id, jsonTasks);

        assertAll(
                () -> assertThat(getForUser1Response.statusCode()).as(wrongCodeMessage(getForUser1Request)).isEqualTo(OK.getCode()),
                () -> assertThat(jsonTasks.length()).as("Number of tasks").isEqualTo(2),
                () -> assertEquals("Wrong task in response : ",
                        "{" +
                                "\"id\" : \"" + task1Id + "\"," +
                                "\"description\" : \"buy milk\"," +
                                "\"due\" : \"2021-06-30\"" +
                                "}",
                        sortedTasks[0],
                        STRICT),
                () -> assertEquals("Wrong task in response : ",
                        "{" +
                                "\"id\" : \"" + task2Id + "\"," +
                                "\"description\" : \"eat an apple\"" +
                                "}",
                        sortedTasks[1],
                        STRICT)
        );
    }

    private JSONObject[] sortJsonTasks(String task1Id, JSONArray jsonTasks) throws JSONException {
        var jsonTask1 = (JSONObject) jsonTasks.get(1);
        var jsonTask2 = (JSONObject) jsonTasks.get(0);

        var sortedTasks = new JSONObject[2];

        if (task1Id.equals(jsonTask1.getString("id"))) {
            sortedTasks[0] = jsonTask1;
            sortedTasks[1] = jsonTask2;
        } else {
            sortedTasks[0] = jsonTask2;
            sortedTasks[1] = jsonTask1;
        }

        return sortedTasks;
    }

    @Test
    @Timeout(1)
    void shouldReturnTask() throws IOException, InterruptedException, JSONException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        var postTaskRequest = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskResponse = httpClient.send(postTaskRequest, HttpResponse.BodyHandlers.ofString());
        var taskId = getIdOfCreatedTask(postTaskResponse);

        var getRequest = taskRequestBuilder(taskId)
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .GET()
                .build();

        //when
        var getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());
        var jsonTask = getJsonFromResponse(getResponse);

        //then
        assertAll(
                () -> assertThat(getResponse.statusCode()).as(wrongCodeMessage(getRequest)).isEqualTo(OK.getCode()),
                () -> assertEquals("Wrong task in response : ",
                        "{" +
                                "\"id\" : \"" + taskId + "\"," +
                                "\"description\" : \"buy milk\"," +
                                "\"due\" : \"2021-06-30\"" +
                                "}",
                        jsonTask,
                        STRICT)
        );
    }

    private static Stream<Arguments> notFoundRequestsGet() {
        return Stream.of(
                taskGetArguments(ID, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
        ).map(Arguments::get)
                .map(objects -> Arguments.of("Task not found", objects[0], objects[1]));
    }

    private static Stream<Arguments> notFoundRequestsPut() {
        return Stream.of(
                        taskPutArguments(ID, BASE_64_USERNAME + ":" + BASE_64_PASSWORD, taskJson("task"))
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Task not found", objects[0], objects[1]));
    }

    private static Stream<Arguments> notFoundRequestsDelete() {
        return Stream.of(
                        taskDeleteArguments(ID, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                ).map(Arguments::get)
                .map(objects -> Arguments.of("Task not found", objects[0], objects[1]));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("notFoundRequestsGet")
    @Timeout(1)
    void shouldReturnNotFoundGet(String testPrefix, String testName, HttpRequest taskRequest) throws IOException, InterruptedException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        //when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(NOT_FOUND.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("notFoundRequestsPut")
    @Timeout(1)
    void shouldReturnNotFoundPut(String testPrefix, String testName, HttpRequest taskRequest) throws IOException, InterruptedException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        //when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(NOT_FOUND.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("notFoundRequestsDelete")
    @Timeout(1)
    void shouldReturnNotFoundDelete(String testPrefix, String testName, HttpRequest taskRequest) throws IOException, InterruptedException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        //when
        var httpResponse = httpClient.send(taskRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(taskRequest))
                .isEqualTo(NOT_FOUND.getCode());
    }

    private static Stream<Arguments> forbiddenRequestsGet() {
        return Stream.<BiFunction<String, String, HttpRequest>>of(
                (taskId, authHeader) -> (HttpRequest) taskGetArguments(taskId, authHeader).get()[1]
        ).map(biFunction -> {
            var testName = HEADER_AUTH + " = " + BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD;
            return Arguments.of("Access forbidden",
                    testName,
                    (Function<String, HttpRequest>) taskId -> biFunction.apply(taskId, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD));
        });
    }

    private static Stream<Arguments> forbiddenRequestsPut() {
        return Stream.<BiFunction<String, String, HttpRequest>>of(
                (taskId, authHeader) -> (HttpRequest) taskPutArguments(taskId, authHeader, taskJson("eat an apple")).get()[1]
        ).map(biFunction -> {
            var testName = HEADER_AUTH + " = " + BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD;
            return Arguments.of("Access forbidden",
                    testName,
                    (Function<String, HttpRequest>) taskId -> biFunction.apply(taskId, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD));
        });
    }

    private static Stream<Arguments> forbiddenRequestsDelete() {
        return Stream.<BiFunction<String, String, HttpRequest>>of(
                (taskId, authHeader) -> (HttpRequest) taskDeleteArguments(taskId, authHeader).get()[1]
        ).map(biFunction -> {
            var testName = HEADER_AUTH + " = " + BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD;
            return Arguments.of("Access forbidden",
                    testName,
                    (Function<String, HttpRequest>) taskId -> biFunction.apply(taskId, BASE_64_USERNAME_2 + ":" + BASE_64_PASSWORD));
        });
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("forbiddenRequestsGet")
    @Timeout(1)
    void shouldReturnForbiddenStatusGet(String testPrefix,
                                     String testName,
                                     Function<String, HttpRequest> requestForUser2Function) throws IOException, InterruptedException {
        //given
        var user1Request = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(user1Request, HttpResponse.BodyHandlers.ofString());

        var user2Request = userRequestBuilder()
                .POST(ofString(userJson("username2", "password")))
                .build();
        httpClient.send(user2Request, HttpResponse.BodyHandlers.ofString());

        var postTaskForUser1Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskForUser1Response = httpClient.send(postTaskForUser1Request, HttpResponse.BodyHandlers.ofString());
        var taskId = getIdOfCreatedTask(postTaskForUser1Response);

        var httpRequest = requestForUser2Function.apply(taskId);

        //when
        var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(httpRequest))
                .isEqualTo(FORBIDDEN.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("forbiddenRequestsPut")
    @Timeout(1)
    void shouldReturnForbiddenStatusPut(String testPrefix,
                                     String testName,
                                     Function<String, HttpRequest> requestForUser2Function) throws IOException, InterruptedException {
        //given
        var user1Request = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(user1Request, HttpResponse.BodyHandlers.ofString());

        var user2Request = userRequestBuilder()
                .POST(ofString(userJson("username2", "password")))
                .build();
        httpClient.send(user2Request, HttpResponse.BodyHandlers.ofString());

        var postTaskForUser1Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskForUser1Response = httpClient.send(postTaskForUser1Request, HttpResponse.BodyHandlers.ofString());
        var taskId = getIdOfCreatedTask(postTaskForUser1Response);

        var httpRequest = requestForUser2Function.apply(taskId);

        //when
        var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(httpRequest))
                .isEqualTo(FORBIDDEN.getCode());
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("forbiddenRequestsDelete")
    @Timeout(1)
    void shouldReturnForbiddenStatusDelete(String testPrefix,
                                     String testName,
                                     Function<String, HttpRequest> requestForUser2Function) throws IOException, InterruptedException {
        //given
        var user1Request = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(user1Request, HttpResponse.BodyHandlers.ofString());

        var user2Request = userRequestBuilder()
                .POST(ofString(userJson("username2", "password")))
                .build();
        httpClient.send(user2Request, HttpResponse.BodyHandlers.ofString());

        var postTaskForUser1Request = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskForUser1Response = httpClient.send(postTaskForUser1Request, HttpResponse.BodyHandlers.ofString());
        var taskId = getIdOfCreatedTask(postTaskForUser1Response);

        var httpRequest = requestForUser2Function.apply(taskId);

        //when
        var httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertThat(httpResponse.statusCode())
                .as(() -> "[" + testPrefix + " : " + testName + "] " + wrongCodeMessage(httpRequest))
                .isEqualTo(FORBIDDEN.getCode());
    }

    @Test
    @Timeout(1)
    void shouldUpdateTask() throws IOException, InterruptedException, JSONException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        var postTaskRequest = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskResponse = httpClient.send(postTaskRequest, HttpResponse.BodyHandlers.ofString());
        var taskId = getIdOfCreatedTask(postTaskResponse);

        var putRequest = taskRequestBuilder(taskId)
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .PUT(ofString(taskJson("eat an apple")))
                .build();
        var getRequest = taskRequestBuilder(taskId)
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .GET()
                .build();

        //when
        var putResponse = httpClient.send(putRequest, HttpResponse.BodyHandlers.ofString());
        var getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertAll(
                () -> assertThat(putResponse.statusCode()).as(wrongCodeMessage(putRequest)).isEqualTo(OK.getCode()),
                () -> assertThat(getResponse.statusCode()).as(wrongCodeMessage(getRequest)).isEqualTo(OK.getCode()),
                () -> assertEquals("Wrong task in response : ",
                        "{" +
                                "\"id\" : \"" + taskId + "\"," +
                                "\"description\" : \"eat an apple\"" +
                                "}",
                        putResponse.body(),
                        STRICT),
                () -> assertEquals("Wrong task in response : ",
                        "{" +
                                "\"id\" : \"" + taskId + "\"," +
                                "\"description\" : \"eat an apple\"" +
                                "}",
                        putResponse.body(),
                        STRICT)
        );
    }

    @Test
    @Timeout(1)
    void shouldDeleteTask() throws IOException, InterruptedException {
        //given
        var userRequest = userRequestBuilder()
                .POST(ofString(userJson("username", "password")))
                .build();
        httpClient.send(userRequest, HttpResponse.BodyHandlers.ofString());

        var postTaskRequest = taskRequestBuilder()
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .POST(ofString(taskJson("buy milk", "2021-06-30")))
                .build();
        var postTaskResponse = httpClient.send(postTaskRequest, HttpResponse.BodyHandlers.ofString());
        var taskId = getIdOfCreatedTask(postTaskResponse);

        var deleteRequest = taskRequestBuilder(taskId)
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .DELETE()
                .build();
        var getRequest = taskRequestBuilder(taskId)
                .header(HEADER_AUTH, BASE_64_USERNAME + ":" + BASE_64_PASSWORD)
                .GET()
                .build();

        //when
        var deleteResponse = httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
        var getResponse = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString());

        //then
        assertAll(
                () -> assertThat(deleteResponse.statusCode()).as(wrongCodeMessage(deleteRequest)).isEqualTo(OK.getCode()),
                () -> assertThat(getResponse.statusCode()).as(wrongCodeMessage(getRequest)).isEqualTo(NOT_FOUND.getCode())
        );
    }

    private String getIdOfCreatedTask(HttpResponse<String> taskResponse) {
        try {
            return taskResponse.body().split("\"")[3];
        } catch (Exception e) {
            fail("Can't get id of created task", e);
            return null;
        }
    }

    private JSONObject getJsonFromResponse(HttpResponse<String> response) throws JSONException {
        try {
            return new JSONObject(response.body());
        } catch (JSONException e) {
            fail("Can't serialize a response to JSON format", e);
            return null;
        }
    }
}
