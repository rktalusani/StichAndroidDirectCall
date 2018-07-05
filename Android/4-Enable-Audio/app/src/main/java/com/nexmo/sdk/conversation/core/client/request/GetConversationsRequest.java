package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.User;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by rux on 20/02/17.
 * @hide
 */

public class GetConversationsRequest extends Request<RequestHandler<List<Conversation>>, List<GetConversationsRequest.Container>> {
    static final String CONVERSATIONS_GET_REQUEST = "user:conversations";
    static final String CONVERSATIONS_GET_SUCCESS = "user:conversations:success";
    private User self;

    public GetConversationsRequest(User self) {
        super(TYPE.OTHER);
        this.self = self;
    }

    @Override
    protected JSONObject makeJson() throws JSONException {
        return newTaggedResponse();
    }

    @Override
    public String getRequestName() {
        return CONVERSATIONS_GET_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return CONVERSATIONS_GET_SUCCESS;
    }


    @Override
    public List<Container> parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        List<Container> conversationList = new ArrayList<>();
        JSONArray conversations = jsonObject.getJSONArray("body");

        for (int index = 0; index < conversations.length(); index++) {
            JSONObject cBody = conversations.getJSONObject(index);
            cBody.put("user_id", self.getUserId());
            cBody.put("user_name", self.getName());

            Conversation conversation = Conversation.fromJson(cBody);
            conversationList.add(new Container(conversation, conversation.getMember(cBody.optString("member_id"))));
        }

        return conversationList;
    }

    public static class Container {
        public final Member member;
        public final Conversation conversation;

        public Container(Conversation conversation, Member member) {
            this.member = member;
            this.conversation = conversation;
        }
    }

}
