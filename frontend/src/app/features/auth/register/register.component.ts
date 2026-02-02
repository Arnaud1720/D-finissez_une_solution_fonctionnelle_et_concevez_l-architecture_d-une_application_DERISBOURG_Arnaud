import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '@core/services';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private router = inject(Router);
  private authService = inject(AuthService);

  form: FormGroup = this.fb.group({
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
    address: [''],
    role: ['USER', Validators.required] // USER ou EMPLOYEE
  });

  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');
  showPassword = signal(false);

  togglePasswordVisibility(): void {
    this.showPassword.update(v => !v);
  }

  onSubmit(): void {
    if (this.form.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    this.successMessage.set('');

    const isEmployee = this.form.get('role')?.value === 'EMPLOYEE';

    this.authService.register(this.form.value).subscribe({
      next: () => {
        this.isLoading.set(false);
        if (isEmployee) {
          this.successMessage.set(
            'Compte créé avec succès ! Votre demande de compte professionnel est en attente de validation par l\'administrateur. ' +
            'Vous recevrez un email de confirmation une fois votre compte validé.'
          );
          // Pas de redirection automatique pour les professionnels
        } else {
          this.successMessage.set('Compte créé avec succès ! Redirection...');
          setTimeout(() => this.router.navigate(['/auth/login']), 2000);
        }
      },
      error: (err) => {
        this.isLoading.set(false);
        this.errorMessage.set(err.error?.message || 'Erreur lors de l\'inscription');
      }
    });
  }
}
