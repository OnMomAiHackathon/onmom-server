package controller;

import dto.group.GroupCreateRequest;
import dto.group.GroupCreateResponse;
import dto.group.GroupMemberUpdateRequest;
import dto.group.GroupMemberUpdateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import service.OnmomGroupService;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("groups")
public class GroupController {
    private final OnmomGroupService onmomGroupService;

    // 그룹 생성
    @PostMapping
    public ResponseEntity<GroupCreateResponse> createGroup(@RequestBody GroupCreateRequest groupCreateRequest) {
        GroupCreateResponse response = onmomGroupService.createGroup(groupCreateRequest);
        return ResponseEntity.ok(response);
    }


    // 그룹 멤버 수정
    @PostMapping("/{groupId}/members")
    public ResponseEntity<GroupMemberUpdateResponse> updateGroupMembers(
            @PathVariable Long groupId,
            @ModelAttribute GroupMemberUpdateRequest updateRequest) throws IOException {

        GroupMemberUpdateResponse response = onmomGroupService.updateGroupMembers(groupId, updateRequest);
        return ResponseEntity.ok(response);
    }

    // 초대코드 보내기
    @PostMapping("/{groupId}/invite")
    public ResponseEntity<String> sendGroupInvite(@PathVariable Long groupId, @RequestParam String email) {
        String response = onmomGroupService.sendInvite(groupId, email);
        return ResponseEntity.ok(response);
    }
}
