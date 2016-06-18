package edu.buffalo.cse.cse486586.groupmessenger2;

import java.io.Serializable;

/**
 * Created by rachna on 3/3/16.
 */
public class MessageObj implements Serializable,Comparable<MessageObj>{
    int senderId;
    int proposedById;
    int uniqueIdMsg;
    float sequenceNo;
    String msgText;
    String msgType;
    boolean deliveryStatus;
    int maxAgreedSequenceNo;
    int proposalCount;
    boolean crash;
    int crashedAVD;

    public MessageObj() {
    }

    public MessageObj(int senderId, int uniqueIdMsg, int sequenceNo, String msgText, String msgType, boolean deliveryStatus, int maxAgreedSequenceNo, int proposedById, boolean crash, int crashedAVD) {
        this.senderId = senderId;
        this.uniqueIdMsg = uniqueIdMsg;
        this.sequenceNo = sequenceNo;
        this.msgText = msgText;
        this.msgType = msgType;
        this.deliveryStatus = deliveryStatus;
        this.maxAgreedSequenceNo = maxAgreedSequenceNo;
        this.proposedById = proposedById;
        this.crash = crash;
        this.crashedAVD = 0;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getUniqueIdMsg() {
        return uniqueIdMsg;
    }

    public void setUniqueIdMsg(int uniqueIdMsg) {
        this.uniqueIdMsg = uniqueIdMsg;
    }

    public Float getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Float sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getMsgText() {
        return msgText;
    }

    public void setMsgText(String msgText) {
        this.msgText = msgText;
    }

    public boolean isDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(boolean deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public int getMaxAgreedSequenceNo() {
        return maxAgreedSequenceNo;
    }

    public void setMaxAgreedSequenceNo(int maxAgreedSequenceNo) {
        this.maxAgreedSequenceNo = maxAgreedSequenceNo;
    }

    public int getProposedById() {
        return proposedById;
    }

    public void setProposedById(int proposedById) {
        this.proposedById = proposedById;
    }

    public void setSequenceNo(float sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public int getProposalCount() {
        return proposalCount;
    }

    public void setProposalCount(int proposalCount) {
        this.proposalCount = proposalCount;
    }

    public int getCrashedAVD() {
        return crashedAVD;
    }

    public void setCrashedAVD(int crashedAVD) {
        this.crashedAVD = crashedAVD;
    }

    public boolean isCrash() {
        return crash;
    }

    public void setCrash(boolean crash) {
        this.crash = crash;
    }

    @Override
    public String toString() {
        return "MessageObj{" +
                "senderId=" + senderId +
                ", proposedById=" + proposedById +
                ", uniqueIdMsg=" + uniqueIdMsg +
                ", sequenceNo=" + sequenceNo +
                ", msgText='" + msgText + '\'' +
                ", msgType='" + msgType + '\'' +
                ", deliveryStatus=" + deliveryStatus +
                ", maxAgreedSequenceNo=" + maxAgreedSequenceNo +
                ", proposalCount=" + proposalCount +
                ", crash=" + crash +
                ", crashedAVD=" + crashedAVD +
                '}';
    }

    @Override
    public int compareTo(MessageObj another) {
        if (this.sequenceNo < another.sequenceNo) {
            return -1;
        } else if (this.sequenceNo > another.sequenceNo) {
            return 1;
        } else {
            return 0;
        }
    }
}

