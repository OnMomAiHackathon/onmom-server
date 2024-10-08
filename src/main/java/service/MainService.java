package service;

import dto.main.groupImage.GroupImageResponse;
import dto.main.groupImage.MarkImageAsViewedRequest;
import dto.main.groupImage.SendImageRequest;
import entity.group.GroupImage;
import entity.group.ImageView;
import entity.group.OnmomGroup;
import entity.user.OnmomUser;
import lombok.RequiredArgsConstructor;
import mapper.main.GroupImageMapper;
import mapper.util.ResponseMessageMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import repository.group.GroupImageRepository;
import repository.group.GroupRepository;
import repository.group.ImageViewRepository;
import repository.user.UserRepository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MainService {
    private final GroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupImageRepository groupImageRepository;
    private final S3Service s3Service;
    private final ImageViewRepository imageViewRepository;

    public ResponseEntity<Map<String, String>> sendImage(SendImageRequest sendImageRequest) {
        OnmomGroup group = groupRepository.findById(sendImageRequest.getGroupId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 그룹 아이디입니다."));

        OnmomUser user = userRepository.findById(sendImageRequest.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저 아이디입니다."));

        // 그룹에 해당 유저가 속해 있는지 확인
        boolean isContainUser = group.getUsers().contains(user);
        if (!isContainUser) {
            throw new IllegalArgumentException("그룹에 해당 유저가 포함되어 있지 않습니다.");
        }

        // 이미지 업로드 및 저장 로직
        Map<String, String> response = new HashMap<>();
        for (MultipartFile file : sendImageRequest.getImageFiles()) {
            if (!file.isEmpty()) {
                try {
                    // S3의 갤러리 경로에 업로드
                    String imageUrl = s3Service.uploadGalleryImage(file, group.getGroupId());

                    // GroupImage 엔티티 생성 및 저장
                    GroupImage groupImage = GroupImage.builder()
                            .imageUrl(imageUrl)
                            .uploadedAt(LocalDateTime.now())
                            .group(group)
                            .user(user)
                            .build();

                    groupImageRepository.save(groupImage);

                    response.put(file.getOriginalFilename(), imageUrl);

                } catch (IOException e) {
                    throw new RuntimeException("이미지 업로드 중 오류가 발생했습니다.", e);
                }
            }
        }

        return ResponseEntity.ok(response);
    }

    //모든 그룹 이미지 조회
    public List<GroupImageResponse> getAllImagesByGroupId(Long groupId) {
        OnmomGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 그룹 ID입니다."));
        List<GroupImage> groupImages = groupImageRepository.findByGroup(group);

        return groupImages.stream()
                .map(GroupImageMapper::toResponse)
                .collect(Collectors.toList());
    }


    // 특정 GroupImage가 특정 User에 의해 조회되는 것을 '기록'
    public Map<String, String> markImageAsViewed(Long userId, Long imageId) {
        GroupImage groupImage = groupImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 이미지 ID입니다."));
        OnmomUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저 ID입니다."));

        // 이미 보았는지 확인
        boolean alreadyViewed = imageViewRepository.findByGroupImageAndUser(groupImage, user).isEmpty();
        if (alreadyViewed) {
            ImageView imageView = ImageView.builder()
                    .groupImage(groupImage)
                    .user(user)
                    .viewedAt(LocalDateTime.now())
                    .build();
            imageViewRepository.save(imageView);
        }

        return ResponseMessageMapper.ResponseMessage("이미지 아이디 " + imageId + ", 읽기에 성공했습니다.");
    }

    //특정 GroupImage가 특정 User에 의해 조회된 적이 있는지 '확인'
    public Map<String, String> hasUserViewedImage(Long imageId, Long userId) {
        GroupImage groupImage = groupImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 이미지 ID입니다."));
        OnmomUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 유저 ID입니다."));

        boolean hasViewed = !imageViewRepository.findByGroupImageAndUser(groupImage, user).isEmpty();

        return ResponseMessageMapper.ResponseMessage(
                hasViewed
                        ? "이미지를 이미 보았습니다."
                        : "아직 보지 않은 이미지입니다."
        );

    }
}