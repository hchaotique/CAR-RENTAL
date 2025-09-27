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
    userBtn.addEventListener('click', () => {
        showUserMenu();
    });
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

function showUserMenu() {
    // Simple user menu toggle
    let userMenu = document.querySelector('.user-menu');
    
    if (!userMenu) {
        userMenu = document.createElement('div');
        userMenu.className = 'user-menu';
        userMenu.style.cssText = `
            position: absolute;
            top: 100%;
            right: 0;
            background: white;
            border: 1px solid #e9ecef;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.1);
            z-index: 1000;
            min-width: 200px;
            opacity: 0;
            transform: translateY(-10px);
            transition: all 0.2s;
        `;
        
        userMenu.innerHTML = `
            <div style="padding: 10px 0;">
                <a href="#" style="display: block; padding: 12px 20px; color: #333; text-decoration: none; transition: background-color 0.2s;">Đăng nhập</a>
                <a href="#" style="display: block; padding: 12px 20px; color: #333; text-decoration: none; transition: background-color 0.2s;">Đăng ký</a>
                <hr style="margin: 10px 0; border: none; border-top: 1px solid #f0f0f0;">
                <a href="#" style="display: block; padding: 12px 20px; color: #333; text-decoration: none; transition: background-color 0.2s;">Trở thành chủ xe</a>
            </div>
        `;
        
        // Add hover effects
        userMenu.querySelectorAll('a').forEach(link => {
            link.addEventListener('mouseenter', () => {
                link.style.backgroundColor = '#f8f9fa';
            });
            link.addEventListener('mouseleave', () => {
                link.style.backgroundColor = 'transparent';
            });
        });
        
        userBtn.parentElement.style.position = 'relative';
        userBtn.parentElement.appendChild(userMenu);
        
        setTimeout(() => {
            userMenu.style.opacity = '1';
            userMenu.style.transform = 'translateY(0)';
        }, 10);
    } else {
        userMenu.remove();
    }
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

console.log('Turo website loaded successfully!');

// Auth Form Validation Functions
function validateEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

function validatePassword(password) {
    return password.length >= 6;
}

function validatePhone(phone) {
    const phoneRegex = /^(\+84|0)[3|5|7|8|9]{1}[0-9]{8}$/;
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
        const password = document.querySelector('input[name="password"]');
        const confirmPassword = document.querySelector('input[name="confirmPassword"]');
        const email = document.querySelector('input[name="email"]');
        const fullName = document.querySelector('input[name="fullName"]');

        // Real-time password match
        if (confirmPassword && password) {
            confirmPassword.addEventListener('input', () => {
                const matchFeedback = registerForm.querySelector('.password-match');
                if (!matchFeedback) {
                    const feedback = document.createElement('div');
                    feedback.className = 'password-match mt-1';
                    feedback.style.fontSize = '14px';
                    confirmPassword.parentNode.appendChild(feedback);
                    matchFeedback = feedback;
                }
                if (password.value !== confirmPassword.value) {
                    matchFeedback.textContent = 'Mật khẩu không khớp!';
                    matchFeedback.style.color = '#dc3545';
                } else if (confirmPassword.value.length > 0) {
                    matchFeedback.textContent = 'Mật khẩu khớp!';
                    matchFeedback.style.color = '#28a745';
                } else {
                    matchFeedback.textContent = '';
                }
            });
        }

        registerForm.addEventListener('submit', (e) => {
            if (!validateEmail(email.value)) {
                e.preventDefault();
                alert('Email không hợp lệ!');
                return false;
            }
            if (fullName.value.trim().length < 2) {
                e.preventDefault();
                alert('Tên đầy đủ phải có ít nhất 2 ký tự!');
                return false;
            }
            if (!validatePassword(password.value)) {
                e.preventDefault();
                alert('Mật khẩu phải có ít nhất 6 ký tự!');
                return false;
            }
            if (password.value !== confirmPassword.value) {
                e.preventDefault();
                alert('Mật khẩu xác nhận không khớp!');
                return false;
            }
            return true;
        });
    }
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
