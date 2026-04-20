import { CommonModule } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  private readonly http = inject(HttpClient);
  private readonly apiBase = this.resolveApiBase();

  @ViewChild('errorBanner') errorBanner?: ElementRef<HTMLDivElement>;

  readonly title = 'RoomFlow';

  user: AuthResponse | null = null;
  authMode: 'login' | 'register' = 'login';
  pageError = '';
  pageNotice = '';
  catalogLoading = true;
  authLoading = false;
  bookingsLoading = false;
  bookingSubmitting = false;
  availabilityLoading = false;

  locations: LocationItem[] = [];
  tariffs: TariffItem[] = [];
  bookings: BookingItem[] = [];
  payments: PaymentItem[] = [];
  occupiedHours: number[] = [];

  registerForm = {
    email: '',
    legalName: '',
    phone: '',
    password: ''
  };

  loginForm = {
    email: '',
    password: ''
  };

  bookingForm = {
    locationId: null as number | null,
    tariffId: null as number | null,
    bookingDate: this.toInputDate(this.nextFullHour()),
    startHour: this.toHourLabel(this.nextFullHour()),
    endHour: this.toHourLabel(this.addHours(this.nextFullHour(), 2))
  };

  ngOnInit(): void {
    this.loadCatalog();
    this.restoreSession();
  }

  switchAuthMode(mode: 'login' | 'register'): void {
    this.authMode = mode;
    this.clearMessages();
  }

  register(): void {
    this.clearMessages();
    this.authLoading = true;
    this.http.post<AuthResponse>(`${this.apiBase}/auth/register`, this.registerForm, { withCredentials: true }).subscribe({
      next: (user) => {
        this.user = user;
        this.authLoading = false;
        this.pageNotice = 'Профиль создан. Теперь можно выбрать помещение и отправить заявку.';
        this.refreshPrivateData();
      },
      error: (error) => {
        this.authLoading = false;
        this.setPageError(this.extractError(error));
      }
    });
  }

  login(): void {
    this.clearMessages();
    this.authLoading = true;
    this.http.post<AuthResponse>(`${this.apiBase}/auth/login`, this.loginForm, { withCredentials: true }).subscribe({
      next: (user) => {
        this.user = user;
        this.authLoading = false;
        this.pageNotice = `С возвращением, ${user.legalName}.`;
        this.refreshPrivateData();
      },
      error: (error) => {
        this.authLoading = false;
        this.setPageError(this.extractError(error));
      }
    });
  }

  logout(): void {
    this.clearMessages();
    this.http.post<void>(`${this.apiBase}/auth/logout`, {}, { withCredentials: true }).subscribe({
      next: () => {
        this.user = null;
        this.bookings = [];
        this.payments = [];
        this.pageNotice = 'Сессия завершена.';
      },
      error: (error) => {
        this.setPageError(this.extractError(error));
      }
    });
  }

  chooseLocation(locationId: number): void {
    this.bookingForm.locationId = locationId;
    this.loadAvailability();
  }

  onLocationChange(): void {
    this.loadAvailability();
  }

  chooseTariff(tariffId: number): void {
    const tariff = this.tariffs.find((item) => item.tariffId === tariffId);
    if (!tariff || !this.isTariffAvailable(tariff.status)) {
      return;
    }
    this.bookingForm.tariffId = tariffId;
  }

  createBooking(): void {
    if (!this.user) {
      this.setPageError('Сначала войдите в систему.');
      return;
    }
    if (!this.bookingForm.locationId || !this.bookingForm.tariffId) {
      this.setPageError('Выберите помещение и тариф.');
      return;
    }
    const selectedTariff = this.tariffs.find((tariff) => tariff.tariffId === this.bookingForm.tariffId);
    if (!selectedTariff || !this.isTariffAvailable(selectedTariff.status)) {
      this.setPageError('Выбранный тариф сейчас недоступен. Пожалуйста, выберите другой.');
      return;
    }
    if (!this.bookingForm.bookingDate) {
      this.setPageError('Выберите дату аренды.');
      return;
    }
    if (!this.bookingForm.startHour || !this.bookingForm.endHour) {
      this.setPageError('Выберите время начала и окончания.');
      return;
    }

    this.clearMessages();
    this.bookingSubmitting = true;
    this.http.post<BookingItem>(`${this.apiBase}/bookings`, {
      locationId: this.bookingForm.locationId,
      tariffId: this.bookingForm.tariffId,
      bookingStart: `${this.bookingForm.bookingDate}T${this.bookingForm.startHour}`,
      bookingEnd: `${this.bookingForm.bookingDate}T${this.bookingForm.endHour}`
    }, { withCredentials: true }).subscribe({
      next: (booking) => {
        this.bookingSubmitting = false;
        this.pageNotice = `Заявка #${booking.bookingId} создана, платёж #${booking.payment.paymentId} на ${this.formatCurrency(booking.payment.paymentSum)} сформирован автоматически.`;
        this.refreshPrivateData();
      },
      error: (error) => {
        this.bookingSubmitting = false;
        this.setPageError(this.extractError(error));
      }
    });
  }

  onBookingDateChange(): void {
    this.loadAvailability();
  }

  trackById(_: number, item: { locationId?: number; tariffId?: number; bookingId?: number | null; paymentId?: number }): number | undefined {
    return item.locationId ?? item.tariffId ?? item.bookingId ?? item.paymentId;
  }

  availableStartHours(): string[] {
    const location = this.selectedLocation();
    if (!location) {
      return [];
    }

    const openingHour = this.parseHour(location.opening);
    const closingHour = this.parseHour(location.closing);
    const date = this.bookingForm.bookingDate;
    const currentHour = this.minimumHourForDate(date);
    const start = Math.max(openingHour, currentHour);
    const endExclusive = closingHour;
    const hours: string[] = [];

    for (let hour = start; hour < endExclusive; hour += 1) {
      if (this.occupiedHours.includes(hour)) {
        continue;
      }
      if (this.availableEndHoursForStart(hour, closingHour).length === 0) {
        continue;
      }
      hours.push(this.hourNumberToLabel(hour));
    }

    return hours;
  }

  availableEndHours(): string[] {
    const location = this.selectedLocation();
    if (!location || !this.bookingForm.startHour) {
      return [];
    }

    const closingHour = this.parseHour(location.closing);
    const startHour = this.parseHour(this.bookingForm.startHour);
    return this.availableEndHoursForStart(startHour, closingHour);
  }

  formatCurrency(value: number): string {
    return `${new Intl.NumberFormat('ru-RU', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value)} ₽`;
  }

  formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('ru-RU', {
      dateStyle: 'medium',
      timeStyle: 'short',
      hour12: false
    }).format(new Date(value));
  }

  statusClass(status: string): string {
    const normalized = status.toLowerCase();
    if (normalized.includes('unpaid') || normalized.includes('unconfirmed')) {
      return 'is-pending';
    }
    if (normalized.includes('paid') || normalized.includes('confirmed')) {
      return 'is-success';
    }
    if (normalized.includes('processing') || normalized.includes('progress')) {
      return 'is-processing';
    }
    return 'is-pending';
  }

  displayStatus(status: string): string {
    const normalized = status.toLowerCase();
    if (normalized === 'unpaid') {
      return 'Не оплачен';
    }
    if (normalized === 'paid') {
      return 'Оплачен';
    }
    if (normalized === 'processing') {
      return 'В обработке';
    }
    if (normalized === 'confirmed') {
      return 'Подтверждено';
    }
    if (normalized === 'unconfirmed') {
      return 'Не подтверждено';
    }
    if (normalized === 'in progress') {
      return 'Идёт аренда';
    }
    if (normalized === 'finished') {
      return 'Завершено';
    }
    return status;
  }

  displayTariffStatus(status: string): string {
    const normalized = status.toLowerCase();
    if (normalized === 'active' || normalized === 'действует' || normalized === 'доступен') {
      return 'Доступен';
    }
    if (normalized === 'inactive' || normalized === 'архивный' || normalized === 'недоступен') {
      return 'Недоступен';
    }
    return status;
  }

  isTariffAvailable(status: string): boolean {
    const normalized = status.trim().toLowerCase();
    return normalized === 'active' || normalized === 'действует' || normalized === 'доступен';
  }

  displayRole(role: string): string {
    return role.toUpperCase() === 'CLIENT' ? 'Клиент' : role;
  }

  displayLocationType(type: string): string {
    const normalized = type.trim().toLowerCase();
    const translations: Record<string, string> = {
      'conference hall': 'Конференц-зал',
      conference: 'Конференц-зал',
      coworking: 'Рабочее пространство',
      'coworking space': 'Рабочее пространство',
      office: 'Офис',
      meeting: 'Переговорная',
      'meeting room': 'Переговорная'
    };
    return translations[normalized] ?? type;
  }

  displayLocationName(name: string): string {
    const normalized = name.trim().toLowerCase();
    const translations: Record<string, string> = {
      volga: 'Зал «Волга»',
      'aero space loft': 'Пространство «Маяк»',
      'mercury room': 'Комната «Спутник»'
    };
    return translations[normalized] ?? name;
  }

  displayAddress(address: string): string {
    const normalized = address.trim().toLowerCase();
    const translations: Record<string, string> = {
      'samara, molodogvardeyskaya 151': 'Самара, ул. Молодогвардейская, 151',
      'samara, moskovskoye shosse 4': 'Самара, Московское шоссе, 4',
      'samara, gagarina 96': 'Самара, ул. Гагарина, 96'
    };
    return translations[normalized] ?? address;
  }

  displayTariffType(type: string): string {
    const normalized = type.trim().toLowerCase();
    const translations: Record<string, string> = {
      standard: 'Стандартный',
      day: 'Дневной',
      daily: 'Дневной',
      promo: 'Пробный',
      trial: 'Пробный'
    };
    return translations[normalized] ?? type;
  }

  displayTariffName(name: string): string {
    const normalized = name.trim().toLowerCase();
    const translations: Record<string, string> = {
      standard: 'Базовый',
      business: 'Рабочий день',
      'business day': 'Рабочий день',
      start: 'Старт',
      promo: 'Старт'
    };
    return translations[normalized] ?? name;
  }

  todayDate(): string {
    return this.toInputDate(new Date());
  }

  locationImage(location: LocationItem): string {
    const images = [
      'assets/location-conference.svg',
      'assets/location-coworking.svg',
      'assets/location-meeting.svg'
    ];
    return images[(location.locationId - 1) % images.length];
  }

  yandexMapsUrl(location: LocationItem): string {
    const query = `${this.displayLocationName(location.name)} ${this.displayAddress(location.address)}`;
    const params = new URLSearchParams({ text: query });

    if (location.longitude !== null && location.latitude !== null) {
      params.set('ll', `${location.longitude},${location.latitude}`);
      params.set('z', '17');
    }

    return `https://yandex.ru/maps/?${params.toString()}`;
  }

  occupiedHoursText(): string {
    if (this.occupiedHours.length === 0) {
      return 'На выбранную дату свободно всё рабочее время.';
    }

    return `Занятые часы: ${this.occupiedHours.map((hour) => this.hourNumberToLabel(hour)).join(', ')}`;
  }

  selectedLocationName(): string {
    const locationName = this.locations.find((location) => location.locationId === this.bookingForm.locationId)?.name;
    return locationName ? this.displayLocationName(locationName) : 'не выбрано';
  }

  selectedTariffName(): string {
    const tariffName = this.tariffs.find((tariff) => tariff.tariffId === this.bookingForm.tariffId)?.name;
    return tariffName ? this.displayTariffName(tariffName) : 'не выбрано';
  }

  private loadCatalog(): void {
    this.catalogLoading = true;
    forkJoin({
      locations: this.http.get<LocationItem[]>(`${this.apiBase}/locations`, { withCredentials: true }),
      tariffs: this.http.get<TariffItem[]>(`${this.apiBase}/tariffs`, { withCredentials: true })
    }).subscribe({
      next: ({ locations, tariffs }) => {
        this.locations = locations;
        this.tariffs = tariffs;
        this.catalogLoading = false;
        if (!this.bookingForm.locationId && locations.length > 0) {
          this.bookingForm.locationId = locations[0].locationId;
        }
        this.ensureAvailableTariffSelected();
        this.loadAvailability();
      },
      error: (error) => {
        this.catalogLoading = false;
        this.setPageError(this.extractError(error));
      }
    });
  }

  private restoreSession(): void {
    this.http.get<AuthResponse>(`${this.apiBase}/auth/me`, { withCredentials: true }).subscribe({
      next: (user) => {
        this.user = user;
        this.refreshPrivateData();
      },
      error: () => {
        this.user = null;
      }
    });
  }

  private refreshPrivateData(): void {
    if (!this.user) {
      return;
    }

    this.bookingsLoading = true;
    forkJoin({
      bookings: this.http.get<BookingItem[]>(`${this.apiBase}/bookings/my`, { withCredentials: true }),
      payments: this.http.get<PaymentItem[]>(`${this.apiBase}/payments/my`, { withCredentials: true })
    }).subscribe({
      next: ({ bookings, payments }) => {
        this.bookings = bookings;
        this.payments = payments;
        this.bookingsLoading = false;
      },
      error: (error) => {
        this.bookingsLoading = false;
        this.setPageError(this.extractError(error));
      }
    });
  }

  private clearMessages(): void {
    this.pageError = '';
    this.pageNotice = '';
  }

  private loadAvailability(): void {
    if (!this.bookingForm.locationId || !this.bookingForm.bookingDate) {
      this.occupiedHours = [];
      this.syncBookingHours();
      return;
    }

    this.availabilityLoading = true;
    this.http.get<LocationAvailabilityResponse>(
      `${this.apiBase}/locations/${this.bookingForm.locationId}/availability`,
      {
        params: { date: this.bookingForm.bookingDate },
        withCredentials: true
      }
    ).subscribe({
      next: (availability) => {
        this.occupiedHours = availability.occupiedHours;
        this.availabilityLoading = false;
        this.syncBookingHours();
      },
      error: (error) => {
        this.availabilityLoading = false;
        this.occupiedHours = [];
        this.syncBookingHours();
        this.setPageError(this.extractError(error));
      }
    });
  }

  private setPageError(message: string): void {
    this.pageError = message;
    queueMicrotask(() => {
      const banner = this.errorBanner?.nativeElement;
      if (!banner) {
        return;
      }
      banner.scrollIntoView({ behavior: 'smooth', block: 'center' });
      banner.focus({ preventScroll: true });
    });
  }

  private extractError(error: HttpErrorResponse): string {
    if (typeof error.error === 'string' && error.error.trim().length > 0) {
      try {
        const parsed = JSON.parse(error.error) as { message?: string };
        if (parsed.message) {
          return parsed.message;
        }
      } catch {
        return error.error;
      }
    }

    if (error.error && typeof error.error === 'object' && 'message' in error.error) {
      const message = (error.error as { message?: string }).message;
      if (message) {
        return message;
      }
    }

    if (error.status === 0) {
      return 'Сервер недоступен. Проверьте, запущен ли сервер на порту 8080.';
    }

    if (error.status) {
      return `Ошибка ${error.status}: не удалось выполнить запрос.`;
    }

    return 'Не удалось выполнить запрос к серверу.';
  }

  private nextFullHour(): Date {
    const value = new Date();
    value.setMinutes(0, 0, 0);
    value.setHours(value.getHours() + 1);
    return value;
  }

  private addHours(date: Date, hours: number): Date {
    const copy = new Date(date);
    copy.setHours(copy.getHours() + hours);
    return copy;
  }

  private toInputDate(date: Date): string {
    const offset = date.getTimezoneOffset();
    const local = new Date(date.getTime() - offset * 60_000);
    return local.toISOString().slice(0, 10);
  }

  private toHourLabel(date: Date): string {
    return this.hourNumberToLabel(date.getHours());
  }

  private hourNumberToLabel(hour: number): string {
    return `${hour.toString().padStart(2, '0')}:00`;
  }

  private parseHour(value: string): number {
    return Number.parseInt(value.slice(0, 2), 10);
  }

  private selectedLocation(): LocationItem | undefined {
    return this.locations.find((location) => location.locationId === this.bookingForm.locationId);
  }

  private minimumHourForDate(date: string): number {
    const today = this.toInputDate(new Date());
    if (date !== today) {
      return 0;
    }
    return this.nextFullHour().getHours();
  }

  private availableEndHoursForStart(startHour: number, closingHour: number): string[] {
    const hours: string[] = [];

    for (let hour = startHour + 1; hour <= closingHour; hour += 1) {
      const previousHour = hour - 1;
      if (this.occupiedHours.includes(previousHour)) {
        break;
      }
      hours.push(this.hourNumberToLabel(hour));
    }

    return hours;
  }

  syncBookingHours(): void {
    const startHours = this.availableStartHours();
    if (startHours.length === 0) {
      this.bookingForm.startHour = '';
      this.bookingForm.endHour = '';
      return;
    }

    if (!startHours.includes(this.bookingForm.startHour)) {
      this.bookingForm.startHour = startHours[0];
    }

    const endHours = this.availableEndHours();
    if (endHours.length === 0) {
      this.bookingForm.endHour = '';
      return;
    }

    if (!endHours.includes(this.bookingForm.endHour)) {
      this.bookingForm.endHour = endHours[Math.min(1, endHours.length - 1)];
    }
  }

  private ensureAvailableTariffSelected(): void {
    const currentTariff = this.tariffs.find((tariff) => tariff.tariffId === this.bookingForm.tariffId);
    if (currentTariff && this.isTariffAvailable(currentTariff.status)) {
      return;
    }

    const firstAvailableTariff = this.tariffs.find((tariff) => this.isTariffAvailable(tariff.status));
    this.bookingForm.tariffId = firstAvailableTariff?.tariffId ?? null;
  }

  private resolveApiBase(): string {
    const location = globalThis.location;
    if (!location) {
      return '/api';
    }

    if (location.port === '4200' || location.port === '4201') {
      return 'http://localhost:8080/api';
    }

    return `${location.origin}/api`;
  }
}

interface AuthResponse {
  userId: number;
  email: string;
  legalName: string;
  phone: string | null;
  role: string;
}

interface LocationItem {
  locationId: number;
  type: string;
  name: string;
  address: string;
  opening: string;
  closing: string;
  phone: string | null;
  latitude: number | null;
  longitude: number | null;
}

interface TariffItem {
  tariffId: number;
  name: string;
  type: string;
  basePrice: number;
  discount: number;
  status: string;
}

interface PaymentDetails {
  paymentId: number;
  paymentSum: number;
  paymentStatus: string;
  paymentMethod: string;
  paymentCreatedAt: string;
  paymentDueDate: string;
}

interface BookingItem {
  bookingId: number;
  locationId: number;
  locationName: string;
  locationAddress: string;
  tariffId: number;
  tariffName: string;
  bookingStart: string;
  bookingEnd: string;
  bookingDurationHours: number;
  bookingPrice: number;
  bookingStatus: string;
  payment: PaymentDetails;
}

interface PaymentItem {
  paymentId: number;
  bookingId: number | null;
  locationName: string | null;
  tariffName: string;
  paymentSum: number;
  paymentStatus: string;
  paymentMethod: string;
  paymentCreatedAt: string;
  paymentDueDate: string;
}

interface LocationAvailabilityResponse {
  locationId: number;
  date: string;
  occupiedHours: number[];
}
