package com.nexmo.sdk.conversation.client.event;

/**
 * Nexmo API exception that can be caught and handled, encapsulates following information:
 * <li>
 *     <ul>rid: Optional request id for which the error occurred, null if the request was pre-validated client side.</ul>
 *     <ul>type: The error type as received from server, for ex: `text:error-not-joined`</ul>
 *     <ul>cid: Optional conversation id associated with the request.</ul>
 *     <ul>message: human readable description of the exception. If not present, only type will be used.</ul>
 *     <ul>stacktrace: class, method name and line number where exception was thrown. </ul>
 * </li>
 *
 * @author emma tresanszki.
 */
public class NexmoAPIError extends Exception {
    public static final String TAG = NexmoAPIError.class.getSimpleName();

    /** Local error type due to pre-validations. **/
    public static final String NO_USER = "user:invalid";
    public static final String MISSING_PARAMS = "missing-params";
    public static final String INVALID_PARAMS = "invalid-params";
    public static final String CONNECT_ALREADY_IN_PROGRESS = "login:already-connecting";
    public static final String AUDIO_ALREADY_IN_PROGRESS = "audio:already-connecting";
    public static final String AUDIO_MISSING_PERMISSIONS = "audio:missing-permissions";
    public static final String AUDIO_GENERAL_CALL_ERROR = "audio:general-error";
    public static final String UPLOAD_FAILURE = "image:upload-failure";
    public static final String DOWNLOAD_FAILURE = "image:download-failure";
    public static final String IMAGE_DELETE_FAILURE = "image:delete-failure";
    public static final String PERMISSION_REQUIRED = "permission:error";
    public static final String UNEXPECTED_RESPONSE = "unexpected-response"; //or "generic-error"
    public static final String INVALID_ACTION = "invalid-action";
    public static final String USER_ALREADY_LOGGED_IN = "login:user-already-loggedin";
    private String rid;
    private String type;
    private String conversationId;

    /**
     * Returns the request Id for which the error was triggered.
     * @return Request id or null of request was pre-validated on the client.
     */
    public String getRid() {
        return rid;
    }

    /**
     * Returns the type of error.
     * @return String describing the error category.
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the conversation id associated with the request.
     * @return The conversation id or null if request was not related to any conversation.
     */
    public String getConversationId() {
        return conversationId;
    }

    public NexmoAPIError(String type, String message) {
        super(type + " . " + message, new Throwable());
        this.type = type;
    }

    public NexmoAPIError(String type, String conversationId, String message) {
        this(type, message);
        this.type = type;
        this.conversationId = conversationId;
    }

    public NexmoAPIError(String rid, String type, String conversationId, String message) {
        this(type, conversationId, message);
        this.rid = rid;
    }

    @Override
    public String toString(){
        return TAG +
                " rid: " + (this.rid!= null ? this.rid: "") +
                " .type: " + (this.type != null ? this.type : "") +
                " .conversationId: " + (this.conversationId != null ? this.conversationId : "") +
                " .message: " + (this.getMessage() != null ? this.getMessage() : "") +
                " .stacktrace: " + this.getStackTrace()[0].getClassName() + "." +
                this.getStackTrace()[0].getMethodName() + ":" + this.getStackTrace()[0].getLineNumber();
    }

    public static void forward(RequestHandler listener, NexmoAPIError error) {
        listener.onError(error);
    }

    /** Predefined local errors. Local errors will never have a rid because they never reach the network. **/

    public static NexmoAPIError noUserLoggedIn() {
        return new NexmoAPIError(NO_USER, "No user is logged in");
    }

    public static NexmoAPIError onUserAlreadyLoggedIn() {
        return new NexmoAPIError(USER_ALREADY_LOGGED_IN, "User already logged in");
    }

    public static NexmoAPIError noUserLoggedInForConversation(final String conversationId) {
        return new NexmoAPIError(NO_USER, conversationId, "No user is logged in");
    }

    public static NexmoAPIError missingParams() {
        return new NexmoAPIError(MISSING_PARAMS, "Missing params");
    }

    public static NexmoAPIError alreadyConnecting() {
        return new NexmoAPIError(CONNECT_ALREADY_IN_PROGRESS, "Already connecting");
    }

    public static NexmoAPIError audioAlreadyInProgress() {
        return new NexmoAPIError(AUDIO_ALREADY_IN_PROGRESS, "Audio call already in progress");
    }

    public static NexmoAPIError audioMissingPermissions() {
        return new NexmoAPIError(AUDIO_MISSING_PERMISSIONS, "Missing permissions for audio call");
    }

    public static NexmoAPIError permissionRequired(final String conversationId) {
        return new NexmoAPIError(PERMISSION_REQUIRED, conversationId, "Permission required");
    }

    public static NexmoAPIError audioGeneralCallError(String description) {
        return new NexmoAPIError(AUDIO_GENERAL_CALL_ERROR, description);
    }

    /** commonly used when received data cannot be parsed.*/
    public static NexmoAPIError unexpectedResponse(String rid, String conversationId) {
        return new NexmoAPIError(rid, UNEXPECTED_RESPONSE, conversationId, "Unable to process response");
    }

    public static NexmoAPIError invalidAction(String conversationId, String message) {
        return new NexmoAPIError(NexmoAPIError.INVALID_ACTION, conversationId, message);
    }

    /** Image event failures */
    public static NexmoAPIError downloadFailure(String conversationId){
        return new NexmoAPIError(NexmoAPIError.DOWNLOAD_FAILURE, conversationId,
                "Request failed due to cancellation, a connectivity problem or timeout.");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof NexmoAPIError)) return false;

        NexmoAPIError that = (NexmoAPIError) o;

        if (rid != null ? !rid.equals(that.rid) : that.rid != null) return false;
        if (!type.equals(that.type)) return false;
        if (conversationId != null ? !conversationId.equals(that.conversationId) : that.conversationId != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

}
