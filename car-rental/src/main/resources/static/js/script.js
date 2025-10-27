// DOM Elements
const tabs = document.querySelectorAll('.tab');
const searchForm = document.querySelector('.search-form');
const carCards = document.querySelectorAll('.car-card');
const navArrows = document.querySelectorAll('.nav-arrow');
const menuBtn = document.querySelector('.menu-btn');
const userBtn = document.querySelector('.user-btn');
const languageBtn = document.querySelector('.language-btn');

// Tab Navigation
tabs.forEach(tab => {
    tab.addEventListener('click', () => {
        // Remove active class from all tabs
        tabs.forEach(t => t.classList.remove('active'));
        // Add active class to clicked tab
        tab.classList.add('active');
        
        // Add visual feedback
        tab.style.transform = 'scale(0.95)';
        setTimeout(() => {
            tab.style.transform = 'scale(1)';
        }, 150);
    });
});

// Search Form Functionality
if (searchForm) {
    const searchBtn = searchForm.querySelector('.search-btn');
    const inputs = searchForm.querySelectorAll('input, select');
    
    searchBtn.addEventListener('click', (e) => {
        e.preventDefault();
        
        // Collect form data
        const formData = new FormData();
        inputs.forEach(input => {
            if (input.value) {
                formData.append(input.name || input.placeholder, input.value);
            }
        });
        
        // Add loading state
        searchBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
        searchBtn.disabled = true;
        
        // Simulate search (replace with actual API call)
        setTimeout(() => {
            searchBtn.innerHTML = '<i class="fas fa-search"></i>';
            searchBtn.disabled = false;
            
            // Show results (you can implement actual search logic here)
            showSearchResults();
        }, 1500);
    });
    
    // Auto-complete for location input
    const locationInput = searchForm.querySelector('input[placeholder*="Thành phố"]');
    if (locationInput) {
        const suggestions = [
            'Hà Nội, Việt Nam',
            'TP. Hồ Chí Minh, Việt Nam',
            'Đà Nẵng, Việt Nam',
            'Hải Phòng, Việt Nam',
            'Cần Thơ, Việt Nam',
            'Sân bay Nội Bài, Hà Nội',
            'Sân bay Tân Sơn Nhất, TP.HCM',
            'Sân bay Đà Nẵng',
            'Nhà ga Hà Nội',
            'Bến xe Mỹ Đình, Hà Nội'
        ];

        locationInput.addEventListener('input', (e) => {
            const value = e.target.value.toLowerCase();
            if (value.length > 2) {
                showSuggestions(locationInput, suggestions.filter(s =>
                    s.toLowerCase().includes(value)
                ));
            } else {
                hideSuggestions();
            }
        });
    }
}

// Car Card Interactions
carCards.forEach(card => {
    card.addEventListener('click', () => {
        const carTitle = card.querySelector('h3').textContent;
        showCarDetails(carTitle);
    });
    
    // Add hover effect for price
    const priceElement = card.querySelector('.price-value');
    if (priceElement) {
        card.addEventListener('mouseenter', () => {
            priceElement.style.color = '#6366f1';
        });
        
        card.addEventListener('mouseleave', () => {
            priceElement.style.color = '#333';
        });
    }
});

// Navigation Arrows for Car Sections
document.querySelectorAll('.car-section').forEach(section => {
    const prevBtn = section.querySelector('.nav-arrow.prev');
    const nextBtn = section.querySelector('.nav-arrow.next');
    const carGrid = section.querySelector('.car-grid');
    
    if (prevBtn && nextBtn && carGrid) {
        let currentIndex = 0;
        const cardWidth = 300; // Approximate card width + gap
        
        prevBtn.addEventListener('click', () => {
            if (currentIndex > 0) {
                currentIndex--;
                carGrid.style.transform = `translateX(-${currentIndex * cardWidth}px)`;
            }
        });
        
        nextBtn.addEventListener('click', () => {
            const maxIndex = Math.max(0, carGrid.children.length - 4);
            if (currentIndex < maxIndex) {
                currentIndex++;
                carGrid.style.transform = `translateX(-${currentIndex * cardWidth}px)`;
            }
        });
    }
});

// Header Menu Functionality
if (menuBtn) {
    menuBtn.addEventListener('click', () => {
        toggleMobileMenu();
    });
}

if (userBtn) {
    // Tìm menu dropdown THẬT (do Thymeleaf tạo ra)
    const userMenu = userBtn.parentElement.querySelector('.user-dropdown-menu');

    if (userMenu) {
        // Khi bấm vào nút user
        userBtn.addEventListener('click', (e) => {
            // Ngăn sự kiện click lan ra ngoài, tránh làm cửa sổ tự đóng
            e.stopPropagation(); 
            
            // Bật/tắt class 'show' để CSS (file styles.css) có thể điều khiển
            userMenu.classList.toggle('show');
        });

        // Tự động đóng menu nếu người dùng bấm ra ngoài cửa sổ
        window.addEventListener('click', () => {
            if (userMenu.classList.contains('show')) {
                userMenu.classList.remove('show');
            }
        });
    }
}

// Language Selector
if (languageBtn) {
    languageBtn.addEventListener('click', () => {
        toggleLanguageMenu();
    });
}

// Utility Functions
function showSearchResults() {
    // Scroll to results
    document.querySelector('.main-content').scrollIntoView({
        behavior: 'smooth'
    });
    
    // Add highlight effect to car cards
    carCards.forEach((card, index) => {
        setTimeout(() => {
            card.style.animation = 'none';
            card.offsetHeight; // Trigger reflow
            card.style.animation = 'fadeIn 0.6s ease-out';
        }, index * 100);
    });
}

function showSuggestions(input, suggestions) {
    hideSuggestions();
    
    if (suggestions.length === 0) return;
    
    const suggestionsList = document.createElement('div');
    suggestionsList.className = 'suggestions-list';
    suggestionsList.style.cssText = `
        position: absolute;
        top: 100%;
        left: 0;
        right: 0;
        background: white;
        border: 1px solid #e9ecef;
        border-radius: 8px;
        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
        z-index: 1000;
        max-height: 200px;
        overflow-y: auto;
    `;
    
    suggestions.forEach(suggestion => {
        const item = document.createElement('div');
        item.className = 'suggestion-item';
        item.textContent = suggestion;
        item.style.cssText = `
            padding: 12px;
            cursor: pointer;
            border-bottom: 1px solid #f8f9fa;
            transition: background-color 0.2s;
        `;
        
        item.addEventListener('mouseenter', () => {
            item.style.backgroundColor = '#f8f9fa';
        });
        
        item.addEventListener('mouseleave', () => {
            item.style.backgroundColor = 'white';
        });
        
        item.addEventListener('click', () => {
            input.value = suggestion;
            hideSuggestions();
        });
        
        suggestionsList.appendChild(item);
    });
    
    input.parentElement.style.position = 'relative';
    input.parentElement.appendChild(suggestionsList);
}

function hideSuggestions() {
    const existingSuggestions = document.querySelector('.suggestions-list');
    if (existingSuggestions) {
        existingSuggestions.remove();
    }
}

function showCarDetails(carTitle) {
    // Create modal for car details
    const modal = document.createElement('div');
    modal.className = 'car-modal';
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0,0,0,0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 2000;
        opacity: 0;
        transition: opacity 0.3s;
    `;
    
    const modalContent = document.createElement('div');
    modalContent.style.cssText = `
        background: white;
        border-radius: 12px;
        padding: 30px;
        max-width: 500px;
        width: 90%;
        text-align: center;
        transform: scale(0.8);
        transition: transform 0.3s;
    `;
    
    modalContent.innerHTML = `
        <h2 style="margin-bottom: 20px; color: #333;">${carTitle}</h2>
        <p style="color: #666; margin-bottom: 20px;">Chi tiết xe đã chọn. Ở đây bạn sẽ thấy thêm thông tin, hình ảnh, và tùy chọn đặt xe.</p>
        <button class="close-modal" style="
            background: #6366f1;
            color: white;
            border: none;
            padding: 12px 24px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 16px;
        ">Đóng</button>
    `;
    
    modal.appendChild(modalContent);
    document.body.appendChild(modal);
    
    // Animate modal
    setTimeout(() => {
        modal.style.opacity = '1';
        modalContent.style.transform = 'scale(1)';
    }, 10);
    
    // Close modal functionality
    const closeBtn = modalContent.querySelector('.close-modal');
    const closeModal = () => {
        modal.style.opacity = '0';
        modalContent.style.transform = 'scale(0.8)';
        setTimeout(() => {
            document.body.removeChild(modal);
        }, 300);
    };
    
    closeBtn.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });
    
    // Close on Escape key
    const handleEscape = (e) => {
        if (e.key === 'Escape') {
            closeModal();
            document.removeEventListener('keydown', handleEscape);
        }
    };
    document.addEventListener('keydown', handleEscape);
}

function toggleMobileMenu() {
    // Create or toggle mobile menu
    let mobileMenu = document.querySelector('.mobile-menu');
    
    if (!mobileMenu) {
        mobileMenu = document.createElement('div');
        mobileMenu.className = 'mobile-menu';
        mobileMenu.style.cssText = `
            position: fixed;
            top: 70px;
            left: 0;
            right: 0;
            background: white;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            z-index: 999;
            transform: translateY(-100%);
            transition: transform 0.3s;
        `;
        
        mobileMenu.innerHTML = `
            <div style="padding: 20px;">
                <a href="#" style="display: block; padding: 15px 0; color: #333; text-decoration: none; border-bottom: 1px solid #f0f0f0;">Tại sao chọn Turo</a>
                <a href="#" style="display: block; padding: 15px 0; color: #333; text-decoration: none; border-bottom: 1px solid #f0f0f0;">Trở thành chủ xe</a>
                <a href="#" style="display: block; padding: 15px 0; color: #333; text-decoration: none; border-bottom: 1px solid #f0f0f0;">Trợ giúp</a>
                <a href="#" style="display: block; padding: 15px 0; color: #333; text-decoration: none;">Đăng nhập</a>
            </div>
        `;
        
        document.body.appendChild(mobileMenu);
    }
    
    const isVisible = mobileMenu.style.transform === 'translateY(0px)';
    mobileMenu.style.transform = isVisible ? 'translateY(-100%)' : 'translateY(0px)';
}


function toggleLanguageMenu() {
    const chevron = languageBtn.querySelector('.fa-chevron-up');
    const isOpen = chevron.style.transform === 'rotate(180deg)';
    
    chevron.style.transform = isOpen ? 'rotate(0deg)' : 'rotate(180deg)';
    
    // You can add language selection logic here
    console.log('Language menu toggled');
}

// Smooth scrolling for anchor links
document.querySelectorAll('a[href^="#"]').forEach(anchor => {
    anchor.addEventListener('click', function (e) {
        e.preventDefault();
        const target = document.querySelector(this.getAttribute('href'));
        if (target) {
            target.scrollIntoView({
                behavior: 'smooth',
                block: 'start'
            });
        }
    });
});

// Lazy loading for images
const images = document.querySelectorAll('img');
const imageObserver = new IntersectionObserver((entries, observer) => {
    entries.forEach(entry => {
        if (entry.isIntersecting) {
            const img = entry.target;

            // Nếu ảnh chưa được nạp, thiết lập src
            if (!img.hasAttribute('data-src-loaded') && img.dataset.src) {
                img.src = img.dataset.src;
                img.setAttribute('data-src-loaded', 'true');
            }

            // Ẩn rồi hiện ảnh khi nó chạm vào viewport
            img.style.opacity = '0';
            img.style.transition = 'opacity 0.5s ease-in-out';

            // Kiểm tra xem ảnh đã nạp hay chưa
            if (img.complete) {
                img.style.opacity = '1';
            } else {
                img.onload = () => {
                    img.style.opacity = '1';
                };
            }

            observer.unobserve(img);
        }
    });
});

// Chuẩn bị ảnh với data-src thay vì src trực tiếp
images.forEach(img => {
    const src = img.src;
    img.dataset.src = src;
    img.src = ''; // Để trống src ban đầu
    img.style.opacity = '0'; // Ẩn ảnh trước khi nạp
    imageObserver.observe(img);
});

// Header scroll effect
let lastScrollY = window.scrollY;
const header = document.querySelector('.header');

window.addEventListener('scroll', () => {
    const currentScrollY = window.scrollY;
    
    if (currentScrollY > lastScrollY && currentScrollY > 100) {
        // Scrolling down
        header.style.transform = 'translateY(-100%)';
    } else {
        // Scrolling up
        header.style.transform = 'translateY(0)';
    }
    
    lastScrollY = currentScrollY;
});

// Add loading states and error handling
window.addEventListener('load', () => {
    // Remove any loading states
    document.body.classList.add('loaded');
    
    // Add entrance animations
    const sections = document.querySelectorAll('.car-section');
    const sectionObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.animation = 'fadeIn 0.8s ease-out';
            }
        });
    });
    
    sections.forEach(section => {
        sectionObserver.observe(section);
    });
});

// Error handling for failed image loads
images.forEach(img => {
    img.addEventListener('error', () => {
        img.src = 'https://via.placeholder.com/300x200/f0f0f0/999999?text=Hình+ảnh+không+dùng+được';
        img.alt = 'Hình ảnh không dùng được';
    });
});

// Keyboard navigation support
document.addEventListener('keydown', (e) => {
    if (e.key === 'Tab') {
        document.body.classList.add('keyboard-navigation');
    }
});

document.addEventListener('mousedown', () => {
    document.body.classList.remove('keyboard-navigation');
});

// Performance optimization: Debounce scroll events
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

// Apply debouncing to scroll handler
const debouncedScrollHandler = debounce(() => {
    // Any scroll-based animations or calculations
}, 16); // ~60fps

window.addEventListener('scroll', debouncedScrollHandler);

console.log('Huy Hoàng Auto 88 website loaded successfully!');

// Loading states for forms
function showLoading(buttonId, loadingText = 'Đang xử lý...') {
    const button = document.getElementById(buttonId);
    if (button) {
        button.disabled = true;
        button.innerHTML = `<i class="fas fa-spinner fa-spin me-2"></i>${loadingText}`;
    }
}

function hideLoading(buttonId, originalText) {
    const button = document.getElementById(buttonId);
    if (button) {
        button.disabled = false;
        button.innerHTML = originalText;
    }
}

// Error handling
function showError(message, containerId = 'error-container') {
    let container = document.getElementById(containerId);
    if (!container) {
        container = document.createElement('div');
        container.id = containerId;
        container.className = 'alert alert-danger mt-3';
        const form = document.querySelector('form');
        if (form) {
            form.parentNode.insertBefore(container, form.nextSibling);
        }
    }
    container.innerHTML = `<i class="fas fa-exclamation-triangle me-2"></i>${message}`;
    container.style.display = 'block';
    setTimeout(() => {
        container.style.display = 'none';
    }, 5000);
}

function showSuccess(message, containerId = 'success-container') {
    let container = document.getElementById(containerId);
    if (!container) {
        container = document.createElement('div');
        container.id = containerId;
        container.className = 'alert alert-success mt-3';
        const form = document.querySelector('form');
        if (form) {
            form.parentNode.insertBefore(container, form.nextSibling);
        }
    }
    container.innerHTML = `<i class="fas fa-check-circle me-2"></i>${message}`;
    container.style.display = 'block';
    setTimeout(() => {
        container.style.display = 'none';
    }, 3000);
}

// Form submission with loading states
document.addEventListener('DOMContentLoaded', function() {
    // Add loading states to forms
    const forms = document.querySelectorAll('form');
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            const submitBtn = form.querySelector('button[type="submit"]');
            if (submitBtn && !submitBtn.disabled) {
                const originalText = submitBtn.innerHTML;
                showLoading(submitBtn.id || 'submit-btn', 'Đang xử lý...');

                // Re-enable after 10 seconds as fallback
                setTimeout(() => {
                    hideLoading(submitBtn.id || 'submit-btn', originalText);
                }, 10000);
            }
        });
    });

    // Enhanced search form
    const searchForm = document.querySelector('.search-form');
    if (searchForm) {
        searchForm.addEventListener('submit', function(e) {
            const locationInput = searchForm.querySelector('input[name="location"]');
            if (locationInput && locationInput.value.trim().length < 2) {
                e.preventDefault();
                showError('Vui lòng nhập địa điểm tìm kiếm (ít nhất 2 ký tự)');
                return false;
            }
        });
    }

    // Enhanced car booking
    const bookButtons = document.querySelectorAll('.book-btn');
    bookButtons.forEach(btn => {
        btn.addEventListener('click', function(e) {
            // Could add confirmation dialog here
            const carName = this.closest('.car-card').querySelector('h3').textContent;
            console.log(`Booking initiated for: ${carName}`);
        });
    });

    // Enhanced user dropdown
    const userBtn = document.querySelector('.user-btn');
    const userMenu = document.querySelector('.user-dropdown-menu');

    if (userBtn && userMenu) {
        // Close menu when clicking outside
        document.addEventListener('click', function(e) {
            if (!userBtn.contains(e.target) && !userMenu.contains(e.target)) {
                userMenu.classList.remove('show');
            }
        });

        // Toggle menu on button click
        userBtn.addEventListener('click', function(e) {
            e.stopPropagation();
            userMenu.classList.toggle('show');
        });
    }

    // Enhanced image lazy loading with error handling
    const images = document.querySelectorAll('img[data-src]');
    const imageObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const img = entry.target;
                const src = img.dataset.src;
                if (src) {
                    img.src = src;
                    img.onload = () => {
                        img.style.opacity = '1';
                        img.removeAttribute('data-src');
                    };
                    img.onerror = () => {
                        img.src = 'https://via.placeholder.com/300x200?text=Hình+ảnh+không+có+sẵn';
                        img.style.opacity = '1';
                    };
                    observer.unobserve(img);
                }
            }
        });
    });

    images.forEach(img => {
        img.style.opacity = '0';
        img.style.transition = 'opacity 0.3s ease-in-out';
        imageObserver.observe(img);
    });

    // Enhanced mobile menu
    const menuBtn = document.querySelector('.menu-btn');
    if (menuBtn) {
        menuBtn.addEventListener('click', function() {
            // Create mobile menu if it doesn't exist
            let mobileMenu = document.querySelector('.mobile-menu');
            if (!mobileMenu) {
                mobileMenu = document.createElement('div');
                mobileMenu.className = 'mobile-menu';
                mobileMenu.style.cssText = `
                    position: fixed;
                    top: 70px;
                    left: 0;
                    right: 0;
                    background: white;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                    z-index: 999;
                    padding: 20px;
                    transform: translateY(-100%);
                    transition: transform 0.3s;
                `;
                mobileMenu.innerHTML = `
                    <a href="/" style="display: block; padding: 15px 0; color: #333; text-decoration: none; border-bottom: 1px solid #f0f0f0;">Trang chủ</a>
                    <a href="/search" style="display: block; padding: 15px 0; color: #333; text-decoration: none; border-bottom: 1px solid #f0f0f0;">Tìm xe</a>
                    <a href="/cars/list" style="display: block; padding: 15px 0; color: #333; text-decoration: none; border-bottom: 1px solid #f0f0f0;">Quản lý xe</a>
                    <a href="/login" style="display: block; padding: 15px 0; color: #333; text-decoration: none;">Đăng nhập</a>
                `;
                document.body.appendChild(mobileMenu);
            }

            const isVisible = mobileMenu.style.transform === 'translateY(0px)';
            mobileMenu.style.transform = isVisible ? 'translateY(-100%)' : 'translateY(0px)';
        });
    }

    // Enhanced scroll effects
    let lastScrollY = window.scrollY;
    const header = document.querySelector('.header');

    window.addEventListener('scroll', function() {
        const currentScrollY = window.scrollY;

        if (currentScrollY > lastScrollY && currentScrollY > 100) {
            // Scrolling down - hide header
            header.style.transform = 'translateY(-100%)';
        } else {
            // Scrolling up - show header
            header.style.transform = 'translateY(0)';
        }

        lastScrollY = currentScrollY;
    });

    // Enhanced keyboard navigation
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Tab') {
            document.body.classList.add('keyboard-navigation');
        }
    });

    document.addEventListener('mousedown', function() {
        document.body.classList.remove('keyboard-navigation');
    });

    // Enhanced focus management
    const focusableElements = document.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])');
    focusableElements.forEach(element => {
        element.addEventListener('focus', function() {
            this.style.outline = '2px solid #28C07B';
            this.style.outlineOffset = '2px';
        });

        element.addEventListener('blur', function() {
            this.style.outline = '';
            this.style.outlineOffset = '';
        });
    });

    console.log('Huy Hoàng Auto 88 enhanced UX loaded successfully!');
});

// Auth Form Validation Functions
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function validatePassword(password) {
    return password.length >= 6;
}

function validatePhone(phone) {
    const phoneRegex = /^((\+84|0)[35789]\d{8}|(\d{4}\s\d{3}\s\d{3})|(\d{2}\s\d{3}\s\d{3}\s\d{3}))?$/;
    return phoneRegex.test(phone) || phone === '';
}

function setupLoginValidation() {
    const loginForm = document.querySelector('form[action="/login"]');
    if (loginForm) {
        loginForm.addEventListener('submit', (e) => {
            const email = document.getElementById('username').value;
            const password = document.getElementById('password').value;
            if (!validateEmail(email)) {
                e.preventDefault();
                alert('Email không hợp lệ!');
                return false;
            }
            if (!validatePassword(password)) {
                e.preventDefault();
                alert('Mật khẩu phải có ít nhất 6 ký tự!');
                return false;
            }
            return true;
        });
    }
}

function setupRegisterValidation() {
    const registerForm = document.querySelector('form[action="/register"]');
    if (registerForm) {
        const password = document.getElementById('password');
        const confirmPassword = document.getElementById('confirmPassword');
        const email = document.getElementById('email');
        const fullName = document.getElementById('fullName');
        const phone = document.getElementById('phone');

        // Password toggle functionality
        setupPasswordToggle();

        // Password strength meter
        setupPasswordStrength();

        // Phone number formatting
        setupPhoneFormatting();

        // Real-time password match validation
        if (confirmPassword && password) {
            confirmPassword.addEventListener('input', () => {
                const matchText = document.getElementById('matchText');
                if (password.value !== confirmPassword.value) {
                    matchText.textContent = 'Mật khẩu không khớp!';
                    matchText.style.color = '#dc3545';
                } else if (confirmPassword.value.length > 0) {
                    matchText.textContent = '✓ Mật khẩu khớp!';
                    matchText.style.color = '#28a745';
                } else {
                    matchText.textContent = '';
                }
            });
        }

        registerForm.addEventListener('submit', (e) => {
            // Validate email
            if (!validateEmail(email.value)) {
                e.preventDefault();
                showToast('Email không hợp lệ!', 'error');
                email.focus();
                return false;
            }

            // Validate full name
            if (fullName.value.trim().length < 2) {
                e.preventDefault();
                showToast('Tên đầy đủ phải có ít nhất 2 ký tự!', 'error');
                fullName.focus();
                return false;
            }

            // Validate phone if provided
            if (phone.value && !validatePhone(phone.value)) {
                e.preventDefault();
                showToast('Số điện thoại không hợp lệ! (Định dạng Việt Nam)', 'error');
                phone.focus();
                return false;
            }

            // Validate password strength
            const strength = calculatePasswordStrength(password.value);
            if (strength.score < 2) {
                e.preventDefault();
                showToast('Mật khẩu quá yếu! Cần ít nhất 8 ký tự với chữ hoa, chữ thường và số.', 'error');
                password.focus();
                return false;
            }

            // Validate password match
            if (password.value !== confirmPassword.value) {
                e.preventDefault();
                showToast('Mật khẩu xác nhận không khớp!', 'error');
                confirmPassword.focus();
                return false;
            }

            // Add timezone to form
            const timezoneInput = document.createElement('input');
            timezoneInput.type = 'hidden';
            timezoneInput.name = 'timezone';
            timezoneInput.value = Intl.DateTimeFormat().resolvedOptions().timeZone;
            registerForm.appendChild(timezoneInput);

            return true;
        });
    }
}

function setupPasswordToggle() {
    // Toggle password visibility
    const togglePassword = document.getElementById('togglePassword');
    const toggleConfirmPassword = document.getElementById('toggleConfirmPassword');
    const password = document.getElementById('password');
    const confirmPassword = document.getElementById('confirmPassword');

    if (togglePassword && password) {
        togglePassword.addEventListener('click', () => {
            const type = password.type === 'password' ? 'text' : 'password';
            password.type = type;
            togglePassword.querySelector('i').className = type === 'password' ? 'fas fa-eye' : 'fas fa-eye-slash';
        });
    }

    if (toggleConfirmPassword && confirmPassword) {
        toggleConfirmPassword.addEventListener('click', () => {
            const type = confirmPassword.type === 'password' ? 'text' : 'password';
            confirmPassword.type = type;
            toggleConfirmPassword.querySelector('i').className = type === 'password' ? 'fas fa-eye' : 'fas fa-eye-slash';
        });
    }
}

function setupPasswordStrength() {
    const password = document.getElementById('password');
    const strengthBar = document.getElementById('strengthBar');
    const strengthText = document.getElementById('strengthText');

    if (password && strengthBar && strengthText) {
        password.addEventListener('input', () => {
            const strength = calculatePasswordStrength(password.value);
            strengthBar.style.width = strength.percentage + '%';

            // Remove all classes
            strengthBar.classList.remove('bg-danger', 'bg-warning', 'bg-success');

            // Add appropriate class and text
            if (strength.score === 0) {
                strengthBar.classList.add('bg-danger');
                strengthText.textContent = 'Rất yếu';
                strengthText.style.color = '#dc3545';
            } else if (strength.score === 1) {
                strengthBar.classList.add('bg-warning');
                strengthText.textContent = 'Yếu';
                strengthText.style.color = '#ffc107';
            } else if (strength.score === 2) {
                strengthBar.classList.add('bg-warning');
                strengthText.textContent = 'Trung bình';
                strengthText.style.color = '#ffc107';
            } else if (strength.score === 3) {
                strengthBar.classList.add('bg-success');
                strengthText.textContent = 'Mạnh';
                strengthText.style.color = '#28a745';
            } else {
                strengthBar.classList.add('bg-success');
                strengthText.textContent = 'Rất mạnh';
                strengthText.style.color = '#28a745';
            }
        });
    }
}

function calculatePasswordStrength(password) {
    let score = 0;
    let feedback = [];

    // Length check
    if (password.length >= 8) {
        score += 1;
    } else {
        feedback.push('ít nhất 8 ký tự');
    }

    // Lowercase check
    if (/[a-z]/.test(password)) {
        score += 1;
    } else {
        feedback.push('chữ thường');
    }

    // Uppercase check
    if (/[A-Z]/.test(password)) {
        score += 1;
    } else {
        feedback.push('chữ hoa');
    }

    // Number check
    if (/\d/.test(password)) {
        score += 1;
    } else {
        feedback.push('số');
    }

    // Special character check
    if (/[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>\/?]/.test(password)) {
        score += 1;
    } else {
        feedback.push('ký tự đặc biệt');
    }

    const percentage = Math.min(100, (score / 5) * 100);

    return {
        score: score,
        percentage: percentage,
        feedback: feedback
    };
}

function setupPhoneFormatting() {
    const phoneInput = document.getElementById('phone');
    if (phoneInput) {
        phoneInput.addEventListener('input', (e) => {
            let value = e.target.value.replace(/\D/g, ''); // Remove non-digits

            // Format Vietnamese phone number
            if (value.startsWith('84') && value.length > 2) {
                // International format: +84 XXX XXX XXX
                value = value.replace(/(\d{2})(\d{3})(\d{3})(\d{3})/, '$1 $2 $3 $4');
            } else if (value.startsWith('0') && value.length > 1) {
                // Local format: 0XXX XXX XXX
                value = value.replace(/(\d{4})(\d{3})(\d{3})/, '$1 $2 $3');
            }

            e.target.value = value;
        });

        phoneInput.addEventListener('blur', () => {
            const value = phoneInput.value.replace(/\s/g, '');
            if (value && !validatePhone(value)) {
                phoneInput.style.borderColor = '#dc3545';
            } else {
                phoneInput.style.borderColor = '#dee2e6';
            }
        });
    }
}

function showToast(message, type = 'success') {
    // Remove existing toasts
    const existingToast = document.querySelector('.toast-notification');
    if (existingToast) {
        existingToast.remove();
    }

    // Create toast container
    const toastContainer = document.createElement('div');
    toastContainer.className = 'toast-notification';
    toastContainer.innerHTML = `
        <div class="toast ${type}" role="alert">
            <div class="toast-header">
                <strong class="me-auto">${type === 'success' ? 'Thành công' : 'Lỗi'}</strong>
                <button type="button" class="btn-close" data-bs-dismiss="toast"></button>
            </div>
            <div class="toast-body">
                ${message}
            </div>
        </div>
    `;

    document.body.appendChild(toastContainer);

    // Initialize Bootstrap toast
    const toast = new bootstrap.Toast(toastContainer.querySelector('.toast'));
    toast.show();

    // Auto remove after 5 seconds
    setTimeout(() => {
        if (toastContainer.parentNode) {
            toastContainer.remove();
        }
    }, 5000);
}

function setupProfileValidation() {
    const profileForm = document.querySelector('form[action="/profile"]');
    if (profileForm) {
        const phone = document.querySelector('input[name="phone"]');
        if (phone) {
            phone.addEventListener('blur', () => {
                if (phone.value && !validatePhone(phone.value)) {
                    alert('Số điện thoại không hợp lệ! (Định dạng Việt Nam)');
                }
            });
        }

        profileForm.addEventListener('submit', (e) => {
            const fullName = document.querySelector('input[name="fullName"]');
            if (fullName.value.trim().length < 2) {
                e.preventDefault();
                alert('Tên đầy đủ phải có ít nhất 2 ký tự!');
                return false;
            }
            if (phone.value && !validatePhone(phone.value)) {
                e.preventDefault();
                alert('Số điện thoại không hợp lệ!');
                return false;
            }
            return true;
        });
    }
}

// Initialize auth validation when DOM loaded
document.addEventListener('DOMContentLoaded', () => {
    setupLoginValidation();
    setupRegisterValidation();
    setupProfileValidation();
});
