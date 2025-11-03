// ZeroMonos Multi-Page Application - v2.0
// Last updated: Multi-page migration
// If you see errors, hard refresh: Ctrl+Shift+R (Windows/Linux) or Cmd+Shift+R (Mac)

const API_BASE = 'http://localhost:8080/api';

let currentBookings = [];
let currentToken = null;

document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
    
    // Check if there's a token in URL (for check page)
    const urlParams = new URLSearchParams(window.location.search);
    const tokenFromUrl = urlParams.get('token');
    const checkTokenInput = document.getElementById('check-token');
    if (tokenFromUrl && checkTokenInput) {
        checkTokenInput.value = tokenFromUrl;
        // Auto-submit the check form
        const checkForm = document.getElementById('check-form');
        if (checkForm) {
            setTimeout(() => {
                checkForm.dispatchEvent(new Event('submit', { cancelable: true, bubbles: true }));
            }, 100);
        }
    }
});

function initializeApp() {
    setupForms();
    loadMunicipalities();
    setMinDate();
    
    // Auto-load bookings if on staff page
    if (document.getElementById('staff-view')) {
        loadBookings();
    }
}

function setupForms() {
    const bookingForm = document.getElementById('booking-form');
    if (bookingForm) {
        bookingForm.addEventListener('submit', handleBookingSubmit);
        
        const addItemBtn = document.getElementById('add-item-btn');
        if (addItemBtn) {
            addItemBtn.addEventListener('click', addItemEntry);
        }
        
        bookingForm.addEventListener('reset', () => {
            const itemsContainer = document.getElementById('items-container');
            if (itemsContainer) {
                itemsContainer.innerHTML = '';
                addItemEntry();
            }
        });
    }
    
    const checkForm = document.getElementById('check-form');
    if (checkForm) {
        checkForm.addEventListener('submit', handleCheckSubmit);
    }
}

function setMinDate() {
    const dateInput = document.getElementById('collection-date');
    if (dateInput) {
        const today = new Date();
        today.setDate(today.getDate() + 1);
        const minDate = today.toISOString().split('T')[0];
        dateInput.min = minDate;
    }
}

async function loadMunicipalities() {
    const municipalitySelect = document.getElementById('municipality');
    const filterMunicipalitySelect = document.getElementById('filter-municipality');
    
    if (!municipalitySelect && !filterMunicipalitySelect) {
        return; // No municipality selects on this page
    }
    
    try {
        const response = await fetch(`${API_BASE}/municipalities`);
        
        if (response.ok) {
            const municipalities = await response.json();
            
            municipalities.forEach(municipality => {
                if (municipalitySelect) {
                    const option = document.createElement('option');
                    option.value = municipality;
                    option.textContent = municipality;
                    municipalitySelect.appendChild(option);
                }
                
                if (filterMunicipalitySelect) {
                    const filterOption = document.createElement('option');
                    filterOption.value = municipality;
                    filterOption.textContent = municipality;
                    filterMunicipalitySelect.appendChild(filterOption);
                }
            });
        } else {
            showToast('Failed to load municipalities', 'error');
            console.error('Failed to load municipalities from API');
        }
    } catch (error) {
        console.error('Error loading municipalities:', error);
        showToast('Failed to load municipalities. Please try again.', 'error');
    }
}

function addItemEntry() {
    const itemsContainer = document.getElementById('items-container');
    const itemCount = itemsContainer.children.length;
    
    if (itemCount >= 10) {
        showToast('Maximum 10 items allowed', 'warning');
        return;
    }
    
    const itemEntry = document.createElement('div');
    itemEntry.className = 'item-entry';
    itemEntry.innerHTML = `
        <div class="form-group">
            <label>Item Name *</label>
            <input type="text" class="item-name" placeholder="e.g., Old Mattress" required>
        </div>
        <div class="form-group">
            <label>Description *</label>
            <input type="text" class="item-description" placeholder="e.g., Queen size, blue color" required>
        </div>
        <button type="button" class="btn-remove" onclick="removeItemEntry(this)">Remove</button>
    `;
    
    itemsContainer.appendChild(itemEntry);
    updateRemoveButtons();
}

function removeItemEntry(button) {
    const itemEntry = button.closest('.item-entry');
    itemEntry.remove();
    updateRemoveButtons();
}

function updateRemoveButtons() {
    const itemsContainer = document.getElementById('items-container');
    const removeButtons = itemsContainer.querySelectorAll('.btn-remove');
    
    removeButtons.forEach((btn, index) => {
        btn.disabled = removeButtons.length === 1;
    });
}

async function handleBookingSubmit(e) {
    e.preventDefault();
    
    const municipality = document.getElementById('municipality').value;
    const date = document.getElementById('collection-date').value;
    const timeSlot = document.getElementById('time-slot').value;
    
    const itemEntries = document.querySelectorAll('.item-entry');
    const items = [];
    
    itemEntries.forEach(entry => {
        const name = entry.querySelector('.item-name').value.trim();
        const description = entry.querySelector('.item-description').value.trim();
        
        if (name && description) {
            items.push({ name, description });
        }
    });
    
    if (items.length === 0) {
        showToast('Please add at least one item', 'error');
        return;
    }
    
    const bookingData = {
        municipality,
        date,
        approxTimeSlot: timeSlot,
        items
    };
    
    try {
        const response = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(bookingData)
        });
        
        if (response.ok) {
            const result = await response.json();
            showBookingSuccess(result.token);
            document.getElementById('booking-form').reset();
            
            const itemsContainer = document.getElementById('items-container');
            itemsContainer.innerHTML = '';
            addItemEntry();
        } else {
            const error = await response.text();
            showToast(`Booking failed: ${error}`, 'error');
        }
    } catch (error) {
        console.error('Error creating booking:', error);
        showToast('Failed to create booking. Please try again.', 'error');
    }
}

function showBookingSuccess(token) {
    const modal = document.getElementById('booking-result');
    const tokenDisplay = document.getElementById('booking-token');
    
    tokenDisplay.textContent = token;
    modal.classList.remove('hidden');
}

function closeModal() {
    const modal = document.getElementById('booking-result');
    modal.classList.add('hidden');
}

function copyToken() {
    const tokenElement = document.getElementById('booking-token');
    const token = tokenElement.textContent;
    
    navigator.clipboard.writeText(token).then(() => {
        showToast('Token copied to clipboard!', 'success');
    }).catch(() => {
        showToast('Failed to copy token', 'error');
    });
}

async function handleCheckSubmit(e) {
    e.preventDefault();
    
    const token = document.getElementById('check-token').value.trim();
    
    if (!token) {
        showToast('Please enter a booking token', 'error');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/bookings/${token}`);
        
        if (response.ok) {
            const booking = await response.json();
            displayBookingDetails(booking);
            currentToken = token;
        } else {
            showToast('Booking not found', 'error');
            clearBookingDetails();
        }
    } catch (error) {
        console.error('Error checking booking:', error);
        showToast('Failed to check booking. Please try again.', 'error');
    }
}

function displayBookingDetails(booking) {
    document.getElementById('detail-token').textContent = booking.token;
    document.getElementById('detail-municipality').textContent = booking.municipality;
    document.getElementById('detail-date').textContent = formatDate(booking.date);
    document.getElementById('detail-time').textContent = booking.approxTimeSlot;
    
    const stateElement = document.getElementById('detail-state');
    stateElement.textContent = booking.currentState.state;
    stateElement.className = `detail-value status-badge status-${booking.currentState.state}`;
    
    const itemsContainer = document.getElementById('detail-items');
    itemsContainer.innerHTML = '';
    
    booking.items.forEach(item => {
        const itemCard = document.createElement('div');
        itemCard.className = 'item-card';
        itemCard.innerHTML = `
            <strong>${item.name}</strong><br>
            <span>${item.description}</span>
        `;
        itemsContainer.appendChild(itemCard);
    });
    
    const historyContainer = document.getElementById('detail-history');
    historyContainer.innerHTML = '';
    
    const allStates = [...booking.previousStates, booking.currentState];
    allStates.forEach(state => {
        const historyEntry = document.createElement('div');
        historyEntry.className = 'history-entry';
        historyEntry.innerHTML = `
            <span class="status-badge status-${state.state}">${state.state}</span>
            <span class="history-time">${formatTimestamp(state.timestamp)}</span>
        `;
        historyContainer.appendChild(historyEntry);
    });
    
    document.getElementById('booking-details').classList.remove('hidden');
}

function clearBookingDetails() {
    document.getElementById('booking-details').classList.add('hidden');
    document.getElementById('check-form').reset();
    currentToken = null;
}

async function cancelBooking() {
    if (!currentToken) {
        showToast('No booking selected', 'error');
        return;
    }
    
    if (!confirm('Are you sure you want to cancel this booking?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/bookings/${currentToken}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showToast('Booking cancelled successfully', 'success');
            clearBookingDetails();
        } else {
            showToast('Failed to cancel booking', 'error');
        }
    } catch (error) {
        console.error('Error cancelling booking:', error);
        showToast('Failed to cancel booking. Please try again.', 'error');
    }
}

async function loadBookings() {
    const filterMunicipality = document.getElementById('filter-municipality').value;
    const filterState = document.getElementById('filter-state').value;
    
    let url = `${API_BASE}/staff/bookings`;
    
    if (filterMunicipality) {
        url = `${API_BASE}/municipalities/${filterMunicipality}`;
    }
    
    try {
        const response = await fetch(url);
        
        if (response.ok) {
            let bookings = await response.json();
            
            if (filterState) {
                bookings = bookings.filter(b => b.currentState.state === filterState);
            }
            
            bookings.sort((b, a) => {
                const dateCompare = new Date(a.date) - new Date(b.date);
                if (dateCompare !== 0) return dateCompare;
                return a.approxTimeSlot.localeCompare(b.approxTimeSlot);
            });
            
            currentBookings = bookings;
            displayBookings(bookings);
            updateStatistics(bookings);
        } else {
            showToast('Failed to load bookings', 'error');
        }
    } catch (error) {
        console.error('Error loading bookings:', error);
        showToast('Failed to load bookings. Please try again.', 'error');
    }
}

function displayBookings(bookings) {
    const tbody = document.getElementById('bookings-tbody');
    tbody.innerHTML = '';
    
    if (bookings.length === 0) {
        tbody.innerHTML = '<tr class="loading-row"><td colspan="7">No bookings found</td></tr>';
        return;
    }
    
    bookings.forEach(booking => {
        const row = document.createElement('tr');
        
        const shortToken = booking.token.substring(0, 8) + '...';
        
        row.innerHTML = `
            <td title="${booking.token}">
                ${shortToken}
                <button class="btn-copy" onclick="copyTokenFromTable('${booking.token}', event)" title="Copy full token">📋</button>
            </td>
            <td>${booking.municipality}</td>
            <td>${formatDate(booking.date)}</td>
            <td>${booking.approxTimeSlot}</td>
            <td>${booking.items.length} item(s)</td>
            <td><span class="status-badge status-${booking.currentState.state}">${booking.currentState.state}</span></td>
            <td>
                <button class="action-btn view" onclick="viewBookingDetails('${booking.token}')">View</button>
                <button class="action-btn edit" onclick="openStateModal('${booking.token}', '${booking.currentState.state}')">Update</button>
            </td>
        `;
        
        tbody.appendChild(row);
    });
}

function updateStatistics(bookings) {
    document.getElementById('total-bookings').textContent = bookings.length;
    
    const receivedCount = bookings.filter(b => b.currentState.state === 'RECEIVED').length;
    const inProgressCount = bookings.filter(b => b.currentState.state === 'IN_PROGRESS').length;
    const finishedCount = bookings.filter(b => b.currentState.state === 'FINISHED').length;
    
    document.getElementById('received-count').textContent = receivedCount;
    document.getElementById('in-progress-count').textContent = inProgressCount;
    document.getElementById('finished-count').textContent = finishedCount;
}

function viewBookingDetails(token) {
    // Redirect to check page with token parameter
    window.location.href = `check.html?token=${encodeURIComponent(token)}`;
}

function openStateModal(token, currentState) {
    const modal = document.getElementById('state-modal');
    document.getElementById('modal-token').textContent = token;
    document.getElementById('modal-current-state').textContent = currentState;
    
    const stateSelect = document.getElementById('new-state');
    stateSelect.value = currentState;
    
    currentToken = token;
    modal.classList.remove('hidden');
}

function closeStateModal() {
    const modal = document.getElementById('state-modal');
    modal.classList.add('hidden');
    currentToken = null;
}

async function updateBookingState() {
    if (!currentToken) {
        showToast('No booking selected', 'error');
        return;
    }
    
    const newState = document.getElementById('new-state').value;
    
    try {
        const response = await fetch(`${API_BASE}/bookings/${currentToken}/state`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ state: newState })
        });
        
        if (response.ok) {
            showToast('Booking state updated successfully', 'success');
            closeStateModal();
            loadBookings();
        } else {
            showToast('Failed to update booking state', 'error');
        }
    } catch (error) {
        console.error('Error updating booking state:', error);
        showToast('Failed to update booking state. Please try again.', 'error');
    }
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    
    const date = new Date(dateString);
    const options = { year: 'numeric', month: 'long', day: 'numeric' };
    return date.toLocaleDateString('en-US', options);
}

function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';
    
    const date = new Date(timestamp);
    const options = { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    };
    return date.toLocaleString('en-US', options);
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.remove('hidden');
    
    setTimeout(() => {
        toast.classList.add('hidden');
    }, 4000);
}

function copyTokenFromTable(token, event) {
    event.stopPropagation();
    
    navigator.clipboard.writeText(token).then(() => {
        showToast('Token copied to clipboard!', 'success');
    }).catch(() => {
        showToast('Failed to copy token', 'error');
    });
}

window.onclick = function(event) {
    const resultModal = document.getElementById('booking-result');
    const stateModal = document.getElementById('state-modal');
    
    if (event.target === resultModal) {
        closeModal();
    }
    
    if (event.target === stateModal) {
        closeStateModal();
    }
}
