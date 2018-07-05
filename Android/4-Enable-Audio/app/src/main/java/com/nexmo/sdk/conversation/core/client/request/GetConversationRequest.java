/**
 * iCopyright (c) 2016 Nexmo Inc
 * All rights reserved.
 *
 */

package com.nexmo.sdk.conversation.core.client.request;

import com.nexmo.sdk.conversation.client.Conversation;
import com.nexmo.sdk.conversation.client.Member;
import com.nexmo.sdk.conversation.client.event.RequestHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * @hide
 */
public class GetConversationRequest extends GetEventsBaseRequest<GetConversationRequest.Container> {
    public static final String TAG = GetConversationRequest.class.getSimpleName();

    static final String CONVERSATION_GET_REQUEST = "conversation:get";
    static final String CONVERSATION_GET_SUCCESS = "conversation:get:success";

    private GetConversationRequest(String cid, String startId, String endId, RequestHandler<Conversation> listener) {
        super(TYPE.GET, cid, startId, endId, listener);
    }

    public GetConversationRequest(String cid, RequestHandler<Conversation> listener) {
        this(cid, null, null, listener);
    }


    @Override
    public String getRequestName() {
        return CONVERSATION_GET_REQUEST;
    }

    @Override
    public String getSuccessEventName() {
        return CONVERSATION_GET_SUCCESS;
    }

    @Override
    public Container parse(JSONObject jsonObject, JSONObject body) throws JSONException {
        Conversation conversation = Conversation.fromJson(body);

        ArrayList<Member> members = new ArrayList<>();

        JSONArray membersArray = body.getJSONArray("members");
        for (int index=0 ; index < membersArray.length(); index++) {
            JSONObject memberJSON = membersArray.getJSONObject(index);
            Member member = Member.fromJson(memberJSON);
            members.add(member);
        }

        return new GetConversationRequest.Container(conversation, members);
    }

    public class Container {
        public final List<Member> members;
        public final Conversation conversation;

        public Container(Conversation conversation, ArrayList<Member> members) {
            this.members = members;
            this.conversation = conversation;
        }
    }
}
