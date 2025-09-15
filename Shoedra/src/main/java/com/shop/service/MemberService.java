package com.shop.service;

import com.shop.constant.Role;
import com.shop.entity.Member;
import com.shop.repository.MemberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
@Transactional
@RequiredArgsConstructor // final, @NonNull 변수에 붙으면 자동 주입(Autowired)을 해줍니다.
public class MemberService implements UserDetailsService {

    private final MemberRepository memberRepository; //자동 주입됨
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final Map<String, String> emailCodeMap = new HashMap<>();


    public Member saveMember(Member member) {
        validateDuplicateMember(member);
        // 기본 역할 설정
        if (member.getRole() == null) {
            member.setRole(Role.USER);
        }
        return memberRepository.save(member); // 데이터베이스에 저장을 하라는 명령
    }
    private void validateDuplicateMember(Member member) {
        Member findMember = memberRepository.findByEmail(member.getEmail()).orElse(null);
        if (findMember != null) {
            throw new IllegalStateException("이미 가입된 회원입니다.");
        }
    }

    public boolean isEmailDuplicate(String email) {
        return memberRepository.existsByEmail(email);
    }

    public String sendVerificationCode(String email) throws MessagingException {
        String code = String.format("%08d", new Random().nextInt(100_000_000));
        emailCodeMap.put(email, code);
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setTo(email);
        helper.setSubject("회원가입 인증코드");
        helper.setText("인증코드: " + code, true);
        mailSender.send(message);
        return code;
    }

    // ??
    public boolean verifyCode(String email, String code) {
        String saved = emailCodeMap.get(email);
        return saved != null && saved.equals(code);
    }

    //??
    public void grantAdminAndSetPassword(String email, String rawPassword) {
        Member member = memberRepository.findByEmail(email).orElse(null);
        if (member != null) {
            member.setRole(Role.ADMIN);
            member.setPassword(passwordEncoder.encode(rawPassword));
            memberRepository.save(member);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

        Member member = memberRepository.findByEmail(email).orElse(null);

        if(member == null){
            throw new UsernameNotFoundException(email);
        }

        // ✅ 세션에 이메일 저장
        /*
        // 연욱소스 세션사용
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        request.getSession().setAttribute("email", email);
         */

        //빌더패턴
        return User.builder()
                .username(member.getEmail())
                .password(member.getPassword())
                .roles(member.getRole().name())
                .build();
    }


}
