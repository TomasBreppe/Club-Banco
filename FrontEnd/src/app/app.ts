import { Component, signal, HostListener } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommonModule], 
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('club-banco-front');

  showScrollTop = false;

  @HostListener('window:scroll', [])
  onWindowScroll(): void {
    this.showScrollTop = window.scrollY > 200;
  }

  scrollTop(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    this.showScrollTop = false;
  }
}
