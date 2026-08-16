package org.nethergames.social.server.impl.service;

import com.google.protobuf.Any;
import com.google.rpc.Code;
import com.google.rpc.ErrorInfo;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.nethergames.social.data.request.exception.RequestException;
import org.nethergames.social.data.request.party.Party;
import org.nethergames.social.data.request.party.PartyData;
import org.nethergames.social.rpc.*;
import org.nethergames.social.server.Social;
import org.nethergames.social.server.utils.ArgumentlessFunction;

import java.util.List;
import java.util.stream.Collectors;

public class SocialServiceImpl extends SocialServiceGrpc.SocialServiceImplBase {

    @Override
    public void createParty(PartyCreateRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            Party party = Social.getInstance().getPartyManager().createParty(request.getXuid());

            responseObserver.onNext(PartyActionResponse.newBuilder()
                    .setPartyId(party.getId())
                    .build()
            );

            responseObserver.onCompleted();
        });
    }

    @Override
    public void getPublicParties(ListPublicPartiesRequest request, StreamObserver<ListPublicPartiesResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            List<Party> parties = Social.getInstance().getPartyManager().listPublicParties();
            List<org.nethergames.social.rpc.Party> converted = parties.stream().map(Party::toGRPC).toList();

            responseObserver.onNext(ListPublicPartiesResponse.newBuilder().addAllParties(converted).build());
        });
    }

    @Override
    public void joinPublicParty(JoinPublicPartyRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            Social.getInstance().getPartyManager().joinPublicParty(request.getSourceXuid(), request.getTargetPartyId());
            responseObserver.onNext(PartyActionResponse.newBuilder()
                    .setPartyId(request.getTargetPartyId())
                    .build()
            );

            responseObserver.onCompleted();
        });
    }

    @Override
    public void disbandParty(PartyDisbandRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().disbandParty(request.getSourceXuid());

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void kickMember(PartyKickRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().kickFromParty(request.getSourceXuid(), request.getTargetXuid());

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void inviteMember(PartyInviteRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().invitePlayer(request.getSourceXuid(), request.getTargetXuid());

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void getPartyMembers(GetPartyMemberRequest request, StreamObserver<GetPartyMemberResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            Party party = Social.getInstance().getPartyManager().getPlayerPartyNonNull(request.getSourceXuid());
            org.nethergames.social.rpc.Party grpcParty = party.toGRPC();

            responseObserver.onNext(GetPartyMemberResponse.newBuilder()
                    .addAllMembers(grpcParty.getMembersList())
                    .build()
            );

            responseObserver.onCompleted();
        });
    }

    @Override
    public void getParty(GetPartyRequest request, StreamObserver<GetPartyResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            Party party = Social.getInstance().getPartyManager().getPlayerPartyNonNull(request.getSourceXuid());
            org.nethergames.social.rpc.Party grpcParty = party.toGRPC();

            responseObserver.onNext(GetPartyResponse.newBuilder()
                    .setId(party.getId())
                    .addAllMember(grpcParty.getMembersList())
                    .setSettings(grpcParty.getSettings())
                    .build()
            );

            responseObserver.onCompleted();
        });
    }

    @Override
    public void getPartyInvites(GetPartyInvitesRequest request, StreamObserver<GetPartyInvitesResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            List<org.nethergames.social.data.request.party.PartyInvite> invites = Social.getInstance().getPartyManager().getPendingPartyInvites(request.getSourceXuid());

            var builder = GetPartyInvitesResponse.newBuilder();

            invites.forEach(invite -> builder.addInvites(invite.toGRPC()));
            responseObserver.onNext(
                    builder.build()
            );

            responseObserver.onCompleted();
        });
    }

    @Override
    public void declineInvite(PartyInvite request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().acceptInvite(request.getXuid(), request.getPartyId());

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void acceptInvite(PartyInvite request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().invitePlayer(request.getXuid(), request.getPartyId());

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void getPlayerPartyInvites(GetPlayerPartyInvitesRequest request, StreamObserver<GetPartyInvitesResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            List<org.nethergames.social.data.request.party.PartyInvite> invites = Social.getInstance().getPartyManager().getPendingPlayerPartyInvites(request.getSourceXuid());

            var builder = GetPartyInvitesResponse.newBuilder();

            invites.forEach(invite -> builder.addInvites(invite.toGRPC()));
            responseObserver.onNext(
                    builder.build()
            );

            responseObserver.onCompleted();
        });
    }

    @Override
    public void updatePartyPlayerStatus(UpdatePlayerPartyStatusRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().updateMemberStatus(request.getSourceXuid(), request.getTargetXuid(), PartyData.MemberStatus.VALUES[request.getStatus().ordinal()]);

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void leaveParty(LeavePartyRequest request, StreamObserver<PartyActionResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            var party = Social.getInstance().getPartyManager().leaveParty(request.getXuid());

            responseObserver.onNext(PartyActionResponse.newBuilder().setPartyId(party.getId()).build());
            responseObserver.onCompleted();
        });
    }

    @Override
    public void getPlayerParty(GetPlayerPartyRequest request, StreamObserver<GetPlayerPartyResponse> responseObserver) {
        requestWrapper(responseObserver, () -> {
            Party party = Social.getInstance().getPartyManager().getPlayerPartyNonNull(request.getXuid());

            responseObserver.onNext(
                    GetPlayerPartyResponse.newBuilder()
                            .setParty(party.toGRPC())
                            .build());
            responseObserver.onCompleted();
        });
    }


    public void requestWrapper(StreamObserver<?> responseObserver, ArgumentlessFunction function) {
        try {
            function.apply();
        } catch (Throwable t) {
            if (t instanceof RequestException ex) {
                com.google.rpc.Status status = Status.newBuilder()
                        .setCode(ex.getErrorCode().getNumber())
                        .setMessage(ex.getIdentifier())
                        .addDetails(
                                Any.pack(
                                        ErrorInfo.newBuilder()
                                                .setReason(ex.getMessage())
                                                .setDomain("org.nethergames.social.party")
                                                .build()
                                )
                        ).build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            } else {
                com.google.rpc.Status status = Status.newBuilder()
                        .setCode(Code.INTERNAL.getNumber())
                        .setMessage(t.getMessage())
                        .addDetails(
                                Any.pack(
                                        ErrorInfo.newBuilder()
                                                .setReason(t.getClass().getName())
                                                .setDomain("org.nethergames.social.party")
                                                .build()
                                )
                        ).build();
                responseObserver.onError(StatusProto.toStatusRuntimeException(status));
            }
        }

    }
}
