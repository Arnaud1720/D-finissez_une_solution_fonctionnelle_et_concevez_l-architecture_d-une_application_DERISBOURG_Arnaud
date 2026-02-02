import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '@core/services';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss'
})
export class ProfileComponent implements OnInit {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  currentUser = this.authService.currentUser;

  profileForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    address: ['']
  });

  deleteForm: FormGroup = this.fb.group({
    password: ['', [Validators.required, Validators.minLength(6)]]
  });

  isEditing = signal(false);
  isLoading = signal(false);
  isDeleting = signal(false);
  showDeleteModal = signal(false);
  successMessage = signal('');
  errorMessage = signal('');
  deleteError = signal('');

  ngOnInit(): void {
    this.loadUserData();
  }

  loadUserData(): void {
    const user = this.currentUser();
    if (user) {
      this.profileForm.patchValue({
        firstName: user.firstName,
        lastName: user.lastName,
        email: user.email,
        address: user.address || ''
      });
    }
  }

  toggleEdit(): void {
    this.isEditing.update(v => !v);
    this.successMessage.set('');
    this.errorMessage.set('');
    if (!this.isEditing()) {
      this.loadUserData();
    }
  }

  onSaveProfile(): void {
    if (this.profileForm.invalid) return;

    this.isLoading.set(true);
    this.successMessage.set('');
    this.errorMessage.set('');

    this.authService.updateProfile(this.profileForm.value).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.isEditing.set(false);
        this.successMessage.set('Profil mis à jour avec succès');
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Erreur lors de la mise à jour');
      }
    });
  }

  openDeleteModal(): void {
    this.showDeleteModal.set(true);
    this.deleteError.set('');
    this.deleteForm.reset();
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
  }

  onDeleteAccount(): void {
    if (this.deleteForm.invalid) return;

    this.isDeleting.set(true);
    this.deleteError.set('');

    this.authService.deleteAccount(this.deleteForm.value.password).subscribe({
      next: () => {
        this.isDeleting.set(false);
      },
      error: (err) => {
        this.isDeleting.set(false);
        this.deleteError.set(err.error?.message || 'Mot de passe incorrect');
      }
    });
  }
}
