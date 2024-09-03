package controller;

import dto.group.*;
import dto.group.expel.GroupExpelMemberRequest;
import dto.group.invite.InviteAcceptRequest;
import dto.group.invite.InviteAcceptResponse;
import entity.group.OnmomGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.OnmomGroupService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("groups")
public class GroupController {
    private final OnmomGroupService onmomGroupService;

    // 그룹 생성
    @PostMapping
    public ResponseEntity<GroupCreateResponse> createGroup(@ModelAttribute GroupCreateRequest groupCreateRequest) {
        GroupCreateResponse response;
        try {
            response = onmomGroupService.createGroup(groupCreateRequest);
        } catch (IOException e) {
            throw new RuntimeException("그룹이미지 업로드 중 장애 발생", e);
        }
        return ResponseEntity.ok(response);
    }




    // 그룹 멤버 수정
    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberUpdateResponse> updateGroupMembers(
            @PathVariable Long groupId,
            @RequestBody GroupMemberUpdateRequest updateRequest) throws IOException {

        GroupMemberUpdateResponse response = onmomGroupService.updateGroupMembers(groupId, updateRequest);
        return ResponseEntity.ok(response);
    }


    // 초대코드 보내기
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<Map<String,String>> sendGroupInvite(@PathVariable Long groupId) {
        String inviteCode = onmomGroupService.sendInvite(groupId);
        // 응답 데이터 구성
        Map<String, String> response = new HashMap<>();
        response.put("message", inviteCode);

        return ResponseEntity.ok(response);
    }

    // 가족 초대 수락
    @PostMapping("/invite/accept")
    public ResponseEntity<InviteAcceptResponse> acceptInvite(
            @RequestBody InviteAcceptRequest request) {
        InviteAcceptResponse response = onmomGroupService.acceptInvite(request);
        return ResponseEntity.ok(response);
    }

    // 그룹 삭제
    @PostMapping("/{groupId}/delete")
    public ResponseEntity<Map<String,String>> deleteGroup(@PathVariable Long groupId){
        onmomGroupService.deleteGroup(groupId);
        Map<String,String> response = new HashMap<>();
        response.put("message","그룹이 성공적으로 삭제되었습니다.");
        return ResponseEntity.ok(response);
    }

    // 특정 그룹 조회
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupFindByIdResponse> findGroupById(@PathVariable Long groupId){
        OnmomGroup group = onmomGroupService.findGroupById(groupId);

        List<GroupUserResponse> members = group.getUsers().stream()
                .map(user -> GroupUserResponse.builder()
                        .userId(user.getUserId())
                        .name(user.getName())
                        .role(user.getRole())
                        .profileImageUrl(user.getProfileImageUrl())
                        .gender(user.getGender())
                        .build()
                ).toList();

        GroupFindByIdResponse response = GroupFindByIdResponse.builder()
                .groupId(group.getGroupId())
                .groupName(group.getGroupName())
                .groupOwnerUserId(group.getGroupOwnerUserId())
                .createdAt(group.getCreatedAt())
                .groupImageUrl(group.getGroupImageUrl())
                .members(members)
                .build();
        return ResponseEntity.ok(response);

    }

    // 그룹장이 그룹원을 그룹에서 추방하는 API
    @PostMapping("/{groupId}/expel")
    public ResponseEntity<Map<String, String>> expelMember(
            @PathVariable Long groupId,
            @RequestBody GroupExpelMemberRequest groupExpelMemberRequest
    ) {
        Map<String, String> response = new HashMap<>();
        try {
            onmomGroupService.expelMember(groupId, groupExpelMemberRequest);
            response.put("message", "그룹원이 성공적으로 추방되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("message", "추방 실패: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


}
